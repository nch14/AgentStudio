package com.chenhaonee.agents.claudecode.process;

import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ClaudeCodeProcessTest {

    @TempDir
    Path tempDir;

    @Test
    void shouldReplayBufferedEventsAndSupportMultipleTurns() throws Exception {
        Path script = createFakeClaudeScript();
        ClaudeCodeProcess process = new ClaudeCodeProcess(List.of("/bin/sh", script.toString()), tempDir.toString());
        process.start();
        try {
            List<StreamJsonEvent> firstTurn = collectTurn(process.startTurn("hello"));
            List<StreamJsonEvent> secondTurn = collectTurn(process.startTurn("world"));

            assertEquals(3, firstTurn.size());
            assertEquals(3, secondTurn.size());

            assertEquals("stream_event", firstTurn.get(0).type());
            assertEquals("message_start", firstTurn.get(0).streamEventType());
            assertEquals("turn-1", secondTurnText(firstTurn));
            assertEquals("turn-2", secondTurnText(secondTurn));

            assertEquals("result", firstTurn.get(2).type());
            assertEquals("result", secondTurn.get(2).type());
        } finally {
            process.shutdownGracefully(1000L);
        }
    }

    private List<StreamJsonEvent> collectTurn(reactor.core.publisher.Flux<StreamJsonEvent> turnFlux) throws Exception {
        Thread.sleep(120L);
        List<StreamJsonEvent> events = turnFlux.collectList().block(Duration.ofSeconds(2));
        assertNotNull(events);
        return events;
    }

    private String secondTurnText(List<StreamJsonEvent> events) {
        return events.get(1).streamDeltaText();
    }

    private Path createFakeClaudeScript() throws IOException {
        Path script = tempDir.resolve("fake-claude.sh");
        Files.writeString(script, """
                #!/bin/sh
                turn=0
                while IFS= read -r line
                do
                  turn=$((turn + 1))
                  printf '{"type":"stream_event","event":{"type":"message_start","message":{"id":"msg_%s","type":"message","role":"assistant","content":[]}},"session_id":"provider-session-%s"}\\n' "$turn" "$turn"
                  printf '{"type":"stream_event","event":{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"turn-%s"}},"session_id":"provider-session-%s"}\\n' "$turn" "$turn"
                  printf '{"type":"result","subtype":"success","session_id":"provider-session-%s","is_error":false}\\n' "$turn"
                done
                """);
        try {
            Files.setPosixFilePermissions(script, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.OWNER_EXECUTE
            ));
        } catch (UnsupportedOperationException ignored) {
            // Windows is not a target environment here, but keep the test portable.
        }
        return script;
    }
}
