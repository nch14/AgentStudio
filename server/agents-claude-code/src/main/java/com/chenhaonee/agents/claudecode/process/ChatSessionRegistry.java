package com.chenhaonee.agents.claudecode.process;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Chat session 进程 LRU 复用池。
 *
 * <p>每个 AgentSession（以 sessionCode 标识）对应一个长生命周期的 Claude 子进程。进程状态：
 * <ul>
 *   <li>IDLE：空闲，可被 acquire 或 LRU 淘汰。</li>
 *   <li>ACTIVE：正在处理一个 turn，不可淘汰。</li>
 *   <li>EVICTING：正在被驱逐（关闭进程），即将从池中移除。</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class ChatSessionRegistry {

    private static final Logger log = LoggerFactory.getLogger(ChatSessionRegistry.class);
    private static final long PROCESS_SHUTDOWN_GRACE_MILLIS = 1500L;

    private final ClaudeCodeProperties properties;
    private final ClaudeCodeProcessBuilder processBuilder;

    private final ConcurrentHashMap<String, TrackedChatProcess> pool = new ConcurrentHashMap<>();
    /** 保护 miss 路径下的容量检查与新建操作的原子性。 */
    private final ReentrantLock poolLock = new ReentrantLock();

    // ─── 状态枚举 ──────────────────────────────────────────────────────────────

    enum ChatProcessState {
        IDLE, ACTIVE, EVICTING
    }

    enum TerminateReason {
        USER_EVICT, LRU_EVICT, IDLE_TIMEOUT, HANG_TIMEOUT, DEAD_PROCESS, SHUTDOWN
    }

    record TrackedChatProcess(
            String sessionCode,
            ChatProcessSpec spec,
            AtomicReference<String> providerSessionId,
            ClaudeCodeProcess process,
            AtomicReference<ChatProcessState> state,
            AtomicLong lastAccessMs,
            AtomicLong turnStartMs,
            CompletableFuture<Void> terminationFuture
    ) {
    }

    // ─── 公开 API ─────────────────────────────────────────────────────────────

    /**
     * 获取 session 对应的已连接进程。命中复用，miss 则新建；池满时 LRU 淘汰一个 IDLE 进程。
     *
     * @param providerSessionId Claude 侧 session UUID；为空表示首次会话，启动时不传 resume/session-id
     * @throws TooManyActiveChatsException 池满且全部 ACTIVE
     * @throws SessionBusyException        同 session 已有 ACTIVE turn
     */
    public ClaudeCodeProcess acquire(String sessionCode, ChatProcessSpec spec, String providerSessionId) {
        ChatProcessSpec normalizedSpec = normalizeSpec(spec);
        // 命中路径
        TrackedChatProcess existing = pool.get(sessionCode);
        if (existing != null) {
            return handleHit(existing, sessionCode, normalizedSpec);
        }
        // miss 路径
        return handleMiss(sessionCode, normalizedSpec, providerSessionId);
    }

    /**
     * 一轮 turn 结束（正常完成或 drain 完成），将 session 标回 IDLE。
     */
    public void release(String sessionCode) {
        TrackedChatProcess tp = pool.get(sessionCode);
        if (tp == null) {
            return;
        }
        if (tp.state().compareAndSet(ChatProcessState.ACTIVE, ChatProcessState.IDLE)) {
            tp.lastAccessMs().set(System.currentTimeMillis());
            log.debug("Session released to IDLE: {}", sessionCode);
        }
    }

    /**
     * 回填或更新 sessionCode 对应的 Claude provider sessionId。
     */
    public void bindProviderSessionId(String sessionCode, String providerSessionId) {
        if (StringUtils.isBlank(providerSessionId)) {
            return;
        }
        TrackedChatProcess tp = pool.get(sessionCode);
        if (tp == null) {
            return;
        }
        tp.providerSessionId().set(providerSessionId);
    }

    /**
     * 显式驱逐（UI 归档会话时调用）。若 ACTIVE 则拒绝（返回 false）。
     *
     * @return true 表示驱逐已触发，false 表示 session 不在池中或当前 ACTIVE
     */
    public boolean evict(String sessionCode) {
        TrackedChatProcess tp = pool.get(sessionCode);
        if (tp == null) {
            return false;
        }
        if (tp.state().get() == ChatProcessState.ACTIVE) {
            throw new SessionBusyException(sessionCode);
        }
        return terminate(tp, ChatProcessState.IDLE, TerminateReason.USER_EVICT);
    }

    /**
     * 用户主动中断：通过 stream-json control_request 让 CLI 自身中断当前 turn。
     *
     * <p>相比 destroyForcibly 子进程，此方式更轻量：进程保持存活，session 状态
     * 保留，下一轮请求可直接复用同一进程，无需 {@code --resume}。CLI 收到后
     * 自行 abort 工具调用与思考，最终通过 {@code result} 事件让 turn 自然结束。</p>
     *
     * @return true 表示成功向 CLI 写入中断指令；false 表示 session 不在池中或写入失败
     */
    public boolean interruptActiveTurn(String sessionCode) {
        TrackedChatProcess tp = pool.get(sessionCode);
        if (tp == null) {
            return false;
        }
        if (tp.state().get() != ChatProcessState.ACTIVE) {
            return false;
        }
        return tp.process().interruptActiveTurn();
    }

    /**
     * 返回当前池中的活跃进程数（供监控/度量使用）。
     */
    public int poolSize() {
        return pool.size();
    }

    // ─── 定时任务 ─────────────────────────────────────────────────────────────

    /**
     * 每 30s 扫描：
     * <ul>
     *   <li>IDLE 超过 chatIdleTimeoutSeconds → 驱逐</li>
     *   <li>ACTIVE 超过 chatTimeoutSeconds（hang 检测）→ 强制驱逐</li>
     *   <li>已 dead 进程 → 从池中移除</li>
     * </ul>
     */
    @Scheduled(fixedRate = 30000)
    public void scanIdleTimeout() {
        long now = System.currentTimeMillis();
        long idleThreshold = now - (long) properties.getChatIdleTimeoutSeconds() * 1000;
        long hangThreshold = now - (long) properties.getChatTimeoutSeconds() * 1000;

        for (TrackedChatProcess tp : pool.values()) {
            // 清理已死亡进程
            if (!tp.process().isAlive() && tp.state().get() != ChatProcessState.EVICTING) {
                log.warn("Removing dead process from pool: sessionCode={}, command={}, stderr={}",
                        tp.sessionCode(),
                        tp.process().commandLine(),
                        safeStderrSummary(tp.process()));
                terminate(tp, tp.state().get(), TerminateReason.DEAD_PROCESS);
                continue;
            }

            ChatProcessState state = tp.state().get();

            if (state == ChatProcessState.IDLE && tp.lastAccessMs().get() < idleThreshold) {
                log.info("Evicting idle-timeout sessionCode: {}", tp.sessionCode());
                terminate(tp, ChatProcessState.IDLE, TerminateReason.IDLE_TIMEOUT);
            } else if (state == ChatProcessState.ACTIVE && tp.turnStartMs().get() < hangThreshold) {
                log.warn("Force-evicting hung sessionCode (turn timeout): {}", tp.sessionCode());
                terminate(tp, ChatProcessState.ACTIVE, TerminateReason.HANG_TIMEOUT);
            }
        }
    }

    /**
     * 应用关闭：驱逐所有进程。
     */
    @PreDestroy
    public void evictAll() {
        log.info("Evicting all chat sessions (count: {})", pool.size());
        List<TrackedChatProcess> snapshot = List.copyOf(pool.values());
        for (TrackedChatProcess tp : snapshot) {
            try {
                terminate(tp, tp.state().get(), TerminateReason.SHUTDOWN);
            } catch (Exception e) {
                log.warn("evictAll error for sessionCode={}: {}", tp.sessionCode(), e.getMessage());
            }
        }
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private ClaudeCodeProcess handleHit(TrackedChatProcess tp, String sessionCode, ChatProcessSpec spec) {
        ChatProcessState current = tp.state().get();
        if (current == ChatProcessState.EVICTING) {
            awaitTermination(tp);
            return handleMiss(sessionCode, spec, tp.providerSessionId().get());
        }

        // 死亡进程：走统一终止路径后按 miss 重建
        if (!tp.process().isAlive()) {
            terminate(tp, current, TerminateReason.DEAD_PROCESS);
            awaitTermination(tp);
            log.warn("Dead process removed on acquire hit, will rebuild: sessionCode={}, command={}, stderr={}",
                    sessionCode,
                    tp.process().commandLine(),
                    safeStderrSummary(tp.process()));
            return handleMiss(sessionCode, spec, tp.providerSessionId().get());
        }

        if (current == ChatProcessState.ACTIVE) {
            throw new SessionBusyException(sessionCode);
        }
        assertSpecCompatible(sessionCode, spec, tp.spec());
        // IDLE → CAS → ACTIVE
        if (tp.state().compareAndSet(ChatProcessState.IDLE, ChatProcessState.ACTIVE)) {
            tp.turnStartMs().set(System.currentTimeMillis());
            log.debug("Session reused (IDLE→ACTIVE): {}", sessionCode);
            return tp.process();
        }
        // CAS 失败（并发竞争），重试一次
        if (tp.state().compareAndSet(ChatProcessState.IDLE, ChatProcessState.ACTIVE)) {
            tp.turnStartMs().set(System.currentTimeMillis());
            return tp.process();
        }
        throw new SessionBusyException(sessionCode);
    }

    private ClaudeCodeProcess handleMiss(String sessionCode, ChatProcessSpec spec, String providerSessionId) {
        poolLock.lock();
        try {
            while (true) {
                // DCL：再检查一次是否已被其他线程建好
                TrackedChatProcess existing = pool.get(sessionCode);
                if (existing != null) {
                    return handleHit(existing, sessionCode, spec);
                }

                int maxChat = properties.getMaxChatProcesses();
                if (pool.size() < maxChat) {
                    break;
                }
                // 找 lastAccessMs 最小的 IDLE 进程淘汰
                TrackedChatProcess lru = pool.values().stream()
                        .filter(tp -> tp.state().get() == ChatProcessState.IDLE)
                        .min(Comparator.comparingLong(tp -> tp.lastAccessMs().get()))
                        .orElseThrow(() -> new TooManyActiveChatsException(
                                "Pool full (" + maxChat + ") and all sessions ACTIVE"));
                log.info("LRU evicting sessionCode {} to make room for {}", lru.sessionCode(), sessionCode);
                if (!terminate(lru, ChatProcessState.IDLE, TerminateReason.LRU_EVICT)) {
                    continue;
                }
                awaitTermination(lru);
            }

            TrackedChatProcess tracked = buildAndStartProcess(sessionCode, spec, providerSessionId);
            tracked.state().set(ChatProcessState.ACTIVE);
            tracked.turnStartMs().set(System.currentTimeMillis());
            pool.put(sessionCode, tracked);
            log.info("New chat process started: sessionCode={}, workDir={}", sessionCode, spec.workDir());
            return tracked.process();
        } finally {
            poolLock.unlock();
        }
    }

    private TrackedChatProcess buildAndStartProcess(String sessionCode, ChatProcessSpec spec, String providerSessionId) {
        String resolvedModel = StringUtils.defaultIfBlank(spec.model(), properties.getDefaultModel());
        boolean isResume = StringUtils.isNotBlank(providerSessionId);

        List<String> command = processBuilder.buildChatProcess(
                spec.workDir(), isResume, providerSessionId, resolvedModel, spec.mcpConfigPath(), spec.systemPrompt());

        ClaudeCodeProcess proc = new ClaudeCodeProcess(command, spec.workDir());
        try {
            proc.start();
        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to start Claude process for sessionCode=" + sessionCode + ", command=" + String.join(" ", command),
                    e);
        }

        return new TrackedChatProcess(
                sessionCode,
                new ChatProcessSpec(spec.workDir(), spec.mcpConfigPath(), resolvedModel, spec.systemPrompt()),
                new AtomicReference<>(providerSessionId),
                proc,
                new AtomicReference<>(ChatProcessState.IDLE),
                new AtomicLong(System.currentTimeMillis()),
                new AtomicLong(0L),
                new CompletableFuture<>());
    }

    /**
     * 终止一个进程：CAS 到 EVICTING → 关闭进程 → 从池移除。
     *
     * @param expectedState CAS 的前置状态（正常驱逐用 IDLE，强制驱逐用 ACTIVE）
     */
    private boolean terminate(TrackedChatProcess tracked, ChatProcessState expectedState, TerminateReason reason) {
        if (!tracked.state().compareAndSet(expectedState, ChatProcessState.EVICTING)) {
            return false; // 已被其他线程抢先
        }
        Throwable failure = null;
        try {
            tracked.process().shutdownGracefully(PROCESS_SHUTDOWN_GRACE_MILLIS);
        } catch (RuntimeException e) {
            failure = e;
            log.warn("Terminate process failed: sessionCode={}, reason={}", tracked.sessionCode(), reason, e);
        } finally {
            pool.remove(tracked.sessionCode(), tracked);
            if (failure == null) {
                tracked.terminationFuture().complete(null);
            } else {
                tracked.terminationFuture().completeExceptionally(failure);
            }
        }
        log.info("Session terminated: sessionCode={}, reason={}", tracked.sessionCode(), reason);
        return true;
    }

    private void awaitTermination(TrackedChatProcess tp) {
        try {
            tp.terminationFuture().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Wait termination timeout for sessionCode={}", tp.sessionCode(), e);
        }
    }

    private ChatProcessSpec normalizeSpec(ChatProcessSpec spec) {
        Objects.requireNonNull(spec, "ChatProcessSpec must not be null");
        if (StringUtils.isBlank(spec.workDir())) {
            throw new IllegalArgumentException("workDir must not be blank");
        }
        if (StringUtils.isBlank(spec.mcpConfigPath())) {
            throw new IllegalArgumentException("mcpConfigPath must not be blank");
        }
        return new ChatProcessSpec(
                spec.workDir(),
                spec.mcpConfigPath(),
                StringUtils.defaultIfBlank(spec.model(), properties.getDefaultModel()),
                StringUtils.trimToNull(spec.systemPrompt())
        );
    }

    private void assertSpecCompatible(String sessionCode, ChatProcessSpec requested, ChatProcessSpec actual) {
        if (!actual.equals(requested)) {
            throw new IllegalStateException("Chat process spec mismatch for sessionCode=" + sessionCode);
        }
    }

    private String safeStderrSummary(ClaudeCodeProcess process) {
        String stderr = process.stderrSummary();
        return StringUtils.defaultIfBlank(stderr, "<empty>");
    }
}
