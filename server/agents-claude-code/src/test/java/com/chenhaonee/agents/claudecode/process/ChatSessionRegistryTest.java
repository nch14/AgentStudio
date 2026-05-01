package com.chenhaonee.agents.claudecode.process;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatSessionRegistryTest {

    private static final List<String> CAT_COMMAND = List.of("/bin/sh", "-c", "cat");

    @TempDir
    Path tempDir;

    @Mock
    private ClaudeCodeProcessBuilder processBuilder;

    private ChatSessionRegistry registry;
    private ClaudeCodeProperties properties;

    @BeforeEach
    void setUp() {
        properties = new ClaudeCodeProperties();
        properties.setMaxChatProcesses(2);
        properties.setChatIdleTimeoutSeconds(30);
        properties.setChatTimeoutSeconds(30);
        registry = new ChatSessionRegistry(properties, processBuilder);
    }

    @AfterEach
    void tearDown() {
        if (registry != null) {
            registry.evictAll();
        }
    }

    @Test
    void shouldReuseIdleSessionProcess() throws Exception {
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        ClaudeCodeProcess first = registry.acquire("session-1", spec, null);
        registry.release("session-1");

        ClaudeCodeProcess reused = registry.acquire("session-1", spec, null);

        assertSame(first, reused);
        registry.release("session-1");
    }

    @Test
    void shouldRejectConcurrentAcquireOnSameSession() throws Exception {
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        registry.acquire("session-1", spec, null);

        assertThrows(SessionBusyException.class, () -> registry.acquire("session-1", spec, null));
    }

    @Test
    void shouldEvictLeastRecentlyUsedIdleSessionWhenPoolFull() throws Exception {
        properties.setMaxChatProcesses(1);
        mockChatProcessCommand();
        ChatProcessSpec firstSpec = spec("session-1");
        ChatProcessSpec secondSpec = spec("session-2");

        ClaudeCodeProcess first = registry.acquire("session-1", firstSpec, null);
        registry.release("session-1");

        ClaudeCodeProcess second = registry.acquire("session-2", secondSpec, null);

        assertEquals(1, registry.poolSize());
        assertTrue(waitUntil(() -> !first.isAlive(), Duration.ofSeconds(2)));
        assertTrue(second.isAlive());
        registry.release("session-2");
    }

    @Test
    void shouldRebuildDeadProcessOnAcquireHit() throws Exception {
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        ClaudeCodeProcess first = registry.acquire("session-1", spec, null);
        registry.release("session-1");
        first.close();
        assertTrue(waitUntil(() -> !first.isAlive(), Duration.ofSeconds(2)));

        ClaudeCodeProcess rebuilt = registry.acquire("session-1", spec, null);

        assertNotSame(first, rebuilt);
        registry.release("session-1");
    }

    @Test
    void shouldEvictIdleSessionOnScan() throws Exception {
        properties.setChatIdleTimeoutSeconds(0);
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        ClaudeCodeProcess process = registry.acquire("session-1", spec, null);
        registry.release("session-1");
        Thread.sleep(10L);

        registry.scanIdleTimeout();

        assertEquals(0, registry.poolSize());
        assertTrue(waitUntil(() -> !process.isAlive(), Duration.ofSeconds(2)));
    }

    @Test
    void shouldEvictHungActiveSessionOnScan() throws Exception {
        properties.setChatTimeoutSeconds(0);
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        ClaudeCodeProcess process = registry.acquire("session-1", spec, null);
        Thread.sleep(10L);

        registry.scanIdleTimeout();

        assertEquals(0, registry.poolSize());
        assertTrue(waitUntil(() -> !process.isAlive(), Duration.ofSeconds(2)));
    }

    @Test
    void shouldRemoveDeadSessionOnScan() throws Exception {
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        ClaudeCodeProcess process = registry.acquire("session-1", spec, null);
        registry.release("session-1");
        process.close();
        assertTrue(waitUntil(() -> !process.isAlive(), Duration.ofSeconds(2)));

        registry.scanIdleTimeout();

        assertEquals(0, registry.poolSize());
    }

    @Test
    void shouldReturnBooleanAccordingToEvictResult() throws Exception {
        mockChatProcessCommand();
        ChatProcessSpec spec = spec("session-1");

        assertFalse(registry.evict("missing"));

        ClaudeCodeProcess process = registry.acquire("session-1", spec, null);
        registry.release("session-1");

        assertTrue(registry.evict("session-1"));
        assertEquals(0, registry.poolSize());
        assertTrue(waitUntil(() -> !process.isAlive(), Duration.ofSeconds(2)));
    }

    @Test
    void shouldKeepPoolSizeWithinLimitUnderConcurrentAcquire() throws Exception {
        properties.setMaxChatProcesses(2);
        mockChatProcessCommand();
        ExecutorService executor = Executors.newFixedThreadPool(6);
        try {
            List<Future<?>> futures = List.of(
                    executor.submit(() -> acquireAndRelease("session-1")),
                    executor.submit(() -> acquireAndRelease("session-2")),
                    executor.submit(() -> acquireAndRelease("session-3")),
                    executor.submit(() -> acquireAndRelease("session-4"))
            );
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
            assertTrue(registry.poolSize() <= 2);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void shouldTerminateAllProcessesOnEvictAll() throws Exception {
        mockChatProcessCommand();
        ClaudeCodeProcess first = registry.acquire("session-1", spec("session-1"), null);
        ClaudeCodeProcess second = registry.acquire("session-2", spec("session-2"), null);

        registry.evictAll();

        assertEquals(0, registry.poolSize());
        assertTrue(waitUntil(() -> !first.isAlive(), Duration.ofSeconds(2)));
        assertTrue(waitUntil(() -> !second.isAlive(), Duration.ofSeconds(2)));
    }

    private void acquireAndRelease(String sessionCode) {
        try {
            ClaudeCodeProcess process = registry.acquire(sessionCode, spec(sessionCode), null);
            registry.release(sessionCode);
            if (!process.isAlive()) {
                throw new IllegalStateException("process should still be alive");
            }
        } catch (TooManyActiveChatsException | SessionBusyException ignored) {
            // 并发竞争下允许请求被拒绝，重点验证池大小不会突破上限。
        }
    }

    private ChatProcessSpec spec(String sessionCode) {
        Path workDir = tempDir.resolve(sessionCode);
        Path mcpConfig = workDir.resolve("mcp-config.json");
        try {
            Files.createDirectories(workDir);
            if (Files.notExists(mcpConfig)) {
                Files.createFile(mcpConfig);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new ChatProcessSpec(workDir.toString(), mcpConfig.toString(), "test-model", null);
    }

    private void mockChatProcessCommand() {
        when(processBuilder.buildChatProcess(any(), anyBoolean(), any(), any(), any(), any())).thenReturn(CAT_COMMAND);
    }

    private boolean waitUntil(Check check, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (System.nanoTime() < deadline) {
            if (check.ok()) {
                return true;
            }
            Thread.sleep(20L);
        }
        return check.ok();
    }

    @FunctionalInterface
    private interface Check {
        boolean ok() throws Exception;
    }
}
