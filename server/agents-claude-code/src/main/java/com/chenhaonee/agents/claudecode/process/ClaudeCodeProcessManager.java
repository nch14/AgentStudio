package com.chenhaonee.agents.claudecode.process;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 管理 Task 模式 Claude Code 子进程的生命周期，提供并发上限保护和超时扫描。
 *
 * <p>Chat 模式的进程池由 {@link ChatSessionRegistry} 专门管理。
 */
@Component
@RequiredArgsConstructor
public class ClaudeCodeProcessManager {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeProcessManager.class);

    private final ClaudeCodeProperties properties;
    private final ConcurrentHashMap<String, TrackedProcess> activeProcesses = new ConcurrentHashMap<>();

    /**
     * 注册一个 task 进程。
     */
    public void registerTask(String processId, ClaudeCodeProcess process) {
        if (activeProcesses.size() >= properties.getMaxConcurrentProcesses()) {
            throw new IllegalStateException("Max concurrent task processes reached: " + properties.getMaxConcurrentProcesses());
        }
        activeProcesses.put(processId, new TrackedProcess(process, System.currentTimeMillis()));
        log.info("Registered task process: {} (active: {})", processId, activeProcesses.size());
    }

    /**
     * 移除进程跟踪。
     */
    public void unregister(String processId) {
        TrackedProcess removed = activeProcesses.remove(processId);
        if (removed != null) {
            log.info("Unregistered task process: {} (active: {})", processId, activeProcesses.size());
        }
    }

    /**
     * 定期扫描超时的 task 进程并强制终止。
     */
    @Scheduled(fixedRate = 30000)
    public void scanTimeoutProcesses() {
        long now = System.currentTimeMillis();
        long taskTimeoutMs = properties.getTaskTimeoutSeconds() * 1000L;

        for (var entry : activeProcesses.entrySet()) {
            String id = entry.getKey();
            TrackedProcess tracked = entry.getValue();
            long elapsedMs = now - tracked.startTimeMs();
            if (elapsedMs > taskTimeoutMs) {
                log.warn("Task process {} timed out ({}ms > {}ms), killing", id, elapsedMs, taskTimeoutMs);
                tracked.process().close();
                activeProcesses.remove(id);
            }
        }
    }

    /**
     * 应用关闭时终止所有 task 子进程。
     */
    @PreDestroy
    public void shutdown() {
        log.info("Shutting down all task processes (count: {})", activeProcesses.size());
        for (var entry : activeProcesses.entrySet()) {
            log.info("Terminating task process: {}", entry.getKey());
            entry.getValue().process().close();
        }
        activeProcesses.clear();
    }

    private record TrackedProcess(ClaudeCodeProcess process, long startTimeMs) {
    }
}
