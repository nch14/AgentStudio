package com.chenhaonee.agents.claudecode.process;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClaudeCodeProcessBuilderTest {

    @Test
    void shouldMapLegacyBypassPermissionModeToCurrentCliValue() {
        ClaudeCodeProperties properties = new ClaudeCodeProperties();
        properties.setPermissionMode("bypass");
        ClaudeCodeProcessBuilder builder = new ClaudeCodeProcessBuilder(properties);

        List<String> command = builder.buildChatProcess(
                "/tmp/agent-home",
                false,
                "00000000-0000-0000-0000-000000000001",
                "claude-sonnet-4-20250514",
                "/tmp/agent-home/mcp-config.json",
                null
        );

        assertTrue(command.contains("--permission-mode"));
        assertTrue(command.contains("bypassPermissions"));
        assertTrue(command.contains("--include-partial-messages"));
        assertFalse(command.contains("--cwd"));
    }
}
