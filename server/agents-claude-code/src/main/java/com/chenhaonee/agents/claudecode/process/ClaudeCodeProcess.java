package com.chenhaonee.agents.claudecode.process;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;
import com.chenhaonee.agents.claudecode.stream.StreamJsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 封装单个 Claude Code 子进程的生命周期。
 *
 * <p>支持两种使用模式：
 * <ul>
 *   <li><b>一次性模式（task）</b>：{@link #start(String)} 写入 stdin 后关闭，通过 {@link #streamLines()} 读输出。</li>
 *   <li><b>持久化模式（chat）</b>：{@link #start()} 启动进程，stdin 常开，每轮通过 {@link #startTurn(String)} 写入并返回本轮事件 Flux。</li>
 * </ul>
 */
public class ClaudeCodeProcess {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeProcess.class);
    private static final int TURN_EVENT_REPLAY_LIMIT = 256;

    private final List<String> command;
    private final String workDir;
    private Process process;
    private final List<String> stderrBuffer = new ArrayList<>();

    /** chat 模式下当前活动 turn 的事件 sink。 */
    private final AtomicReference<ActiveTurn> activeTurnRef = new AtomicReference<>();

    private volatile boolean started = false;
    /** 保证同一进程上的 stdin write 串行化。 */
    private final Object turnLock = new Object();
    private Writer stdinWriter;

    private final StreamJsonParser streamJsonParser = new StreamJsonParser();

    private record ActiveTurn(Sinks.Many<StreamJsonEvent> sink, long startedAtMs) {
    }

    public ClaudeCodeProcess(List<String> command, String workDir) {
        this.command = command;
        this.workDir = workDir;
    }

    // ─── 一次性模式（task 模式）────────────────────────────────────────────────

    /**
     * 启动进程，写入 stdin 后立即关闭（task 模式）。
     */
    public void start(String input) throws IOException {
        ProcessBuilder pb = buildProcessBuilder();
        process = pb.start();

        if (input != null && !input.isBlank()) {
            try (OutputStreamWriter writer = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8)) {
                writer.write(input);
            }
        } else {
            process.getOutputStream().close();
        }

        startStderrReader();
        startExitWatcher();
    }

    /**
     * 以 Flux 流式返回 stdout 行（task 模式）。
     */
    public Flux<String> streamLines() {
        return Flux.create(sink -> {
            Thread readerThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null && !sink.isCancelled()) {
                        sink.next(line);
                    }
                    sink.complete();
                } catch (IOException e) {
                    if (!sink.isCancelled()) {
                        sink.error(e);
                    }
                }
            }, "claude-stdout-reader-" + Thread.currentThread().threadId());
            readerThread.setDaemon(true);
            readerThread.start();

            sink.onCancel(() -> log.debug("stream cancelled, reader thread will stop"));
        });
    }

    /**
     * 阻塞等待进程结束（task 模式）。
     */
    public boolean waitFor(long timeoutSeconds) throws InterruptedException {
        if (process == null) {
            return false;
        }
        boolean exited = process.waitFor(timeoutSeconds, java.util.concurrent.TimeUnit.SECONDS);
        if (!exited) {
            log.warn("Process timed out after {}s, killing", timeoutSeconds);
            close();
        }
        return exited;
    }

    // ─── 持久化模式（chat 模式）────────────────────────────────────────────────

    /**
     * 启动进程，stdin 常开，stdout 持续泵入 eventSink（chat 持久化模式）。
     */
    public void start() throws IOException {
        if (started) {
            throw new IllegalStateException("Process already started");
        }
        ProcessBuilder pb = buildProcessBuilder();
        process = pb.start();
        stdinWriter = new OutputStreamWriter(process.getOutputStream(), StandardCharsets.UTF_8);
        started = true;

        startStderrReader();
        startStdoutPump();
        startExitWatcher();
    }

    /**
     * 向 stdin 写入一条 user message（stream-json 格式），返回本轮事件 Flux。
     * 本轮事件由独立 sink 承载，读到 {@code type=result} 后完成。
     */
    public Flux<StreamJsonEvent> startTurn(String userInput) throws IOException {
        JSONArray content = new JSONArray();
        JSONObject textBlock = new JSONObject();
        textBlock.put("type", "text");
        textBlock.put("text", userInput);
        content.add(textBlock);
        return startTurn(content);
    }

    /**
     * 多模态 user content 数组形态。content 元素为 Anthropic 风格的 content block
     * （text / image / document），由调用方组装并已完成 base64 编码。
     */
    public Flux<StreamJsonEvent> startTurn(JSONArray userContent) throws IOException {
        synchronized (turnLock) {
            if (!started || stdinWriter == null) {
                throw new IllegalStateException("Process not started");
            }
            if (process == null || !process.isAlive()) {
                throw new IOException("Claude Code process is not alive");
            }
            if (activeTurnRef.get() != null) {
                throw new IllegalStateException("Another Claude turn is still active");
            }

            ActiveTurn activeTurn = new ActiveTurn(Sinks.many().replay().limit(TURN_EVENT_REPLAY_LIMIT),
                    System.currentTimeMillis());
            activeTurnRef.set(activeTurn);
            String jsonLine = buildUserMessage(userContent);
            try {
                stdinWriter.write(jsonLine);
                stdinWriter.write("\n");
                stdinWriter.flush();
            } catch (IOException | RuntimeException e) {
                failActiveTurn(activeTurn, e);
                throw e;
            }
            return activeTurn.sink().asFlux();
        }
    }

    /**
     * 共享事件 Flux，供外部观察（心跳/错误监听）。
     */
    public Flux<StreamJsonEvent> events() {
        return Flux.defer(() -> {
            ActiveTurn activeTurn = activeTurnRef.get();
            return activeTurn == null ? Flux.empty() : activeTurn.sink().asFlux();
        });
    }

    /**
     * 向 stdin 写入 stream-json control_request 消息，请求 CLI 中断当前 turn。
     *
     * <p>CLI 收到后自行 abort 当前工具调用与模型思考，最终发出 {@code result}
     * 事件让 turn 自然结束。进程保持存活，session 状态保留，下一轮可继续使用
     * 同一进程，无需 {@code --resume}。</p>
     *
     * @return true 表示成功写入；false 表示进程未启动或无活跃 turn 或 IO 失败
     */
    public boolean interruptActiveTurn() {
        synchronized (turnLock) {
            if (!started || stdinWriter == null) {
                return false;
            }
            if (process == null || !process.isAlive()) {
                return false;
            }
            if (activeTurnRef.get() == null) {
                return false;
            }
            String requestId = "req_interrupt_" + System.nanoTime();
            JSONObject request = new JSONObject();
            request.put("subtype", "interrupt");
            JSONObject controlRequest = new JSONObject();
            controlRequest.put("type", "control_request");
            controlRequest.put("request_id", requestId);
            controlRequest.put("request", request);
            String line = JSON.toJSONString(controlRequest);
            try {
                stdinWriter.write(line);
                stdinWriter.write("\n");
                stdinWriter.flush();
                log.info("Sent interrupt control_request: requestId={}", requestId);
                return true;
            } catch (IOException e) {
                log.warn("Failed to write interrupt to stdin", e);
                return false;
            }
        }
    }

    /**
     * 关闭 stdin，让进程优雅退出（chat 模式驱逐时调用）。
     */
    public void closeStdin() {
        if (stdinWriter != null) {
            try {
                stdinWriter.close();
            } catch (IOException e) {
                log.debug("closeStdin error", e);
            }
        }
    }

    /**
     * 先关闭 stdin，等待短暂时间让进程自然退出，超时后再强制终止。
     */
    public void shutdownGracefully(long waitMillis) {
        closeStdin();
        if (process == null || !process.isAlive()) {
            return;
        }
        try {
            boolean exited = process.waitFor(waitMillis, TimeUnit.MILLISECONDS);
            if (!exited) {
                close();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            close();
        }
    }

    // ─── 通用 ─────────────────────────────────────────────────────────────────

    public int exitCode() {
        return process != null ? process.exitValue() : -1;
    }

    public List<String> getStderr() {
        synchronized (stderrBuffer) {
            return new ArrayList<>(stderrBuffer);
        }
    }

    public boolean isAlive() {
        return process != null && process.isAlive();
    }

    public String commandLine() {
        return String.join(" ", command);
    }

    public String stderrSummary() {
        synchronized (stderrBuffer) {
            if (stderrBuffer.isEmpty()) {
                return "";
            }
            StringJoiner joiner = new StringJoiner(System.lineSeparator());
            for (String line : stderrBuffer) {
                joiner.add(line);
            }
            return joiner.toString();
        }
    }

    /**
     * 强制终止进程（hard kill 兜底）。
     */
    public void close() {
        if (process != null && process.isAlive()) {
            log.info("Destroying Claude Code process");
            process.destroyForcibly();
        }
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private ProcessBuilder buildProcessBuilder() {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.directory(new File(workDir));
        pb.redirectErrorStream(false);
        return pb;
    }

    private void startStderrReader() {
        Thread stderrThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    synchronized (stderrBuffer) {
                        stderrBuffer.add(line);
                    }
                }
            } catch (IOException e) {
                log.debug("stderr reader interrupted", e);
            }
        }, "claude-stderr-reader-" + Thread.currentThread().threadId());
        stderrThread.setDaemon(true);
        stderrThread.start();
    }

    /**
     * stdout pump：持续读取、解析并推送到当前 turn sink（持久化模式专用）。
     * 读到 EOF 后结束当前 turn；IO 异常时向当前 turn 传播 error。
     */
    private void startStdoutPump() {
        Thread stdoutThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("Claude stdout raw line: {}", line);
                    StreamJsonEvent event = streamJsonParser.parseLine(line);
                    if (event != null) {
                        log.info("Claude stdout parsed event: type={}, subtype={}, sessionId={}",
                                event.type(), event.subtype(), event.session_id());
                        emitToActiveTurn(event);
                    }
                }
                completeActiveTurnOnPumpExit();
            } catch (IOException e) {
                log.debug("stdout pump interrupted", e);
                failActiveTurnOnPumpError(e);
            }
        }, "claude-stdout-pump-" + Thread.currentThread().threadId());
        stdoutThread.setDaemon(true);
        stdoutThread.start();
    }

    private void startExitWatcher() {
        Thread exitThread = new Thread(() -> {
            try {
                int exitCode = process.waitFor();
                String stderr = stderrSummary();
                if (exitCode == 0) {
                    log.info("Claude Code process exited: exitCode={}, workDir={}", exitCode, workDir);
                    return;
                }
                if (stderr.isBlank()) {
                    log.warn("Claude Code process exited abnormally: exitCode={}, workDir={}, command={}",
                            exitCode, workDir, commandLine());
                    return;
                }
                log.warn("Claude Code process exited abnormally: exitCode={}, workDir={}, command={}, stderr={}",
                        exitCode, workDir, commandLine(), stderr);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.debug("exit watcher interrupted");
            }
        }, "claude-exit-watcher-" + Thread.currentThread().threadId());
        exitThread.setDaemon(true);
        exitThread.start();
    }

    /** 构造 stream-json 格式的 user message 行（不含末尾换行）。 */
    private String buildUserMessage(JSONArray userContent) {
        Map<String, Object> message = Map.of("role", "user", "content", userContent);
        Map<String, Object> wrapper = Map.of("type", "user", "message", message);
        return JSON.toJSONString(wrapper);
    }

    private void emitToActiveTurn(StreamJsonEvent event) {
        ActiveTurn activeTurn = activeTurnRef.get();
        if (activeTurn == null) {
            log.warn("Claude stdout event has no active turn receiver: type={}, subtype={}, sessionId={}, command={}",
                    event.type(), event.subtype(), event.session_id(), commandLine());
            return;
        }
        Sinks.EmitResult emitResult = activeTurn.sink().tryEmitNext(event);
        if (emitResult != Sinks.EmitResult.OK) {
            log.warn("Claude stdout event emit failed: emitResult={}, type={}, subtype={}, sessionId={}, command={}",
                    emitResult, event.type(), event.subtype(), event.session_id(), commandLine());
        }
        if ("result".equals(event.type())) {
            completeActiveTurn(activeTurn);
        }
    }

    private void completeActiveTurnOnPumpExit() {
        ActiveTurn activeTurn = activeTurnRef.getAndSet(null);
        if (activeTurn == null) {
            return;
        }
        Sinks.EmitResult emitResult = activeTurn.sink().tryEmitComplete();
        if (emitResult != Sinks.EmitResult.OK && emitResult != Sinks.EmitResult.FAIL_TERMINATED) {
            log.warn("Claude turn completion emit failed on pump exit: emitResult={}, command={}",
                    emitResult, commandLine());
        }
    }

    private void failActiveTurnOnPumpError(Throwable error) {
        ActiveTurn activeTurn = activeTurnRef.getAndSet(null);
        if (activeTurn == null) {
            return;
        }
        Sinks.EmitResult emitResult = activeTurn.sink().tryEmitError(error);
        if (emitResult != Sinks.EmitResult.OK && emitResult != Sinks.EmitResult.FAIL_TERMINATED) {
            log.warn("Claude turn error emit failed: emitResult={}, command={}", emitResult, commandLine(), error);
        }
    }

    private void failActiveTurn(ActiveTurn activeTurn, Throwable error) {
        if (!activeTurnRef.compareAndSet(activeTurn, null)) {
            return;
        }
        Sinks.EmitResult emitResult = activeTurn.sink().tryEmitError(error);
        if (emitResult != Sinks.EmitResult.OK && emitResult != Sinks.EmitResult.FAIL_TERMINATED) {
            log.warn("Claude turn startup error emit failed: emitResult={}, command={}", emitResult, commandLine(), error);
        }
    }

    private void completeActiveTurn(ActiveTurn activeTurn) {
        if (!activeTurnRef.compareAndSet(activeTurn, null)) {
            return;
        }
        Sinks.EmitResult emitResult = activeTurn.sink().tryEmitComplete();
        if (emitResult != Sinks.EmitResult.OK && emitResult != Sinks.EmitResult.FAIL_TERMINATED) {
            log.warn("Claude turn completion emit failed: emitResult={}, command={}", emitResult, commandLine());
        }
    }
}
