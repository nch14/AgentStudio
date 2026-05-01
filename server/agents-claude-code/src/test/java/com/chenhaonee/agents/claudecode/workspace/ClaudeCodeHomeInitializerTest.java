package com.chenhaonee.agents.claudecode.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.mcp.McpConfigGenerator;
import com.chenhaonee.agents.claudecode.prompt.ClaudeCodePrompts;
import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.spi.context.AgentProfile;
import com.chenhaonee.agents.connect.spi.context.OwnerContext;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClaudeCodeHomeInitializerTest {

    @TempDir
    Path tempDir;

    private ClaudeCodeHomeInitializer initializer;

    @BeforeEach
    void setUp() {
        ClaudeCodeProperties properties = new ClaudeCodeProperties();
        properties.setMcpServerUrl("http://localhost:3000");

        AgentConfigApi agentConfigApi = mock(AgentConfigApi.class);
        AgentProfile profile = new AgentProfile("TestAgent", "Test Responsibility", Map.of());
        OwnerContext owner = new OwnerContext("Test User", null, "Asia/Shanghai", "zh_CN", null,
                "Test User 是一名软件工程师，专注于后端开发与系统架构设计。平时喜欢简洁直接的沟通风格，不喜欢冗余的回答。");
        when(agentConfigApi.getAgentProfile("test-agent")).thenReturn(profile);
        when(agentConfigApi.getOwnerContext()).thenReturn(owner);

        ClaudeCodePrompts prompts = new ClaudeCodePrompts(agentConfigApi);
        McpConfigGenerator mcpGenerator = new McpConfigGenerator();

        initializer = new ClaudeCodeHomeInitializer(properties, prompts, mcpGenerator);
    }

    @Test
    void supportedTypeShouldReturnClaudeCode() {
        assertEquals(AgentProvider.CLAUDE_CODE, initializer.supportedType());
    }

    @Test
    void initHomeShouldWriteHomeFilesAndMcpConfig() throws Exception {
        Path agentHome = tempDir.resolve("test-agent");
        Files.createDirectories(agentHome);

        initializer.initHome("test-agent", agentHome);

        assertTrue(Files.exists(agentHome.resolve("CLAUDE.md")));
        assertTrue(Files.exists(agentHome.resolve("soul.md")));
        assertTrue(Files.exists(agentHome.resolve("owner.md")));
        assertTrue(Files.exists(agentHome.resolve("tool.md")));
        assertTrue(Files.isDirectory(agentHome.resolve("memory")));
        assertTrue(Files.exists(agentHome.resolve("memory").resolve("overview.md")));
        assertTrue(Files.isDirectory(agentHome.resolve("memory").resolve("daily")));
        assertTrue(Files.isDirectory(agentHome.resolve("memory").resolve("topics")));
        assertTrue(Files.exists(agentHome.resolve("mcp-config.json")));
        String claudeMd = Files.readString(agentHome.resolve("CLAUDE.md"));
        assertTrue(claudeMd.contains("# TestAgent"));
        assertTrue(claudeMd.contains("`owner.md`"));
        assertTrue(claudeMd.contains("`memory/overview.md`"));
        assertTrue(Files.readString(agentHome.resolve("tool.md")).contains("task_create(title, content, agentCode)"));
        assertTrue(Files.readString(agentHome.resolve("soul.md")).contains("先解决问题，再说话"));
        String ownerMd = Files.readString(agentHome.resolve("owner.md"));
        assertTrue(ownerMd.contains("Display name: Test User"));
        assertTrue(ownerMd.contains("软件工程师"));
        assertTrue(Files.readString(agentHome.resolve("memory").resolve("overview.md")).contains("Stable Facts"));
    }

    @Test
    void initHomeShouldBeIdempotent() throws Exception {
        Path agentHome = tempDir.resolve("test-agent");
        Files.createDirectories(agentHome);
        String customClaudeContent = "my custom claude content";
        String customSoulContent = "my custom soul content";
        String customOwnerContent = "my custom owner content";
        String customToolContent = "my custom tool content";
        Files.writeString(agentHome.resolve("CLAUDE.md"), customClaudeContent);
        Files.writeString(agentHome.resolve("soul.md"), customSoulContent);
        Files.writeString(agentHome.resolve("owner.md"), customOwnerContent);
        Files.writeString(agentHome.resolve("tool.md"), customToolContent);

        initializer.initHome("test-agent", agentHome);

        assertEquals(customClaudeContent, Files.readString(agentHome.resolve("CLAUDE.md")));
        assertEquals(customSoulContent, Files.readString(agentHome.resolve("soul.md")));
        assertEquals(customOwnerContent, Files.readString(agentHome.resolve("owner.md")));
        assertEquals(customToolContent, Files.readString(agentHome.resolve("tool.md")));
        assertTrue(Files.exists(agentHome.resolve("memory").resolve("overview.md")));
        assertTrue(Files.isDirectory(agentHome.resolve("memory").resolve("daily")));
        assertTrue(Files.isDirectory(agentHome.resolve("memory").resolve("topics")));
    }

    @Test
    void initHomeShouldCreateMissingFilesWithoutOverwritingExistingOnes() throws Exception {
        Path agentHome = tempDir.resolve("test-agent");
        Files.createDirectories(agentHome);
        String customSoulContent = "custom soul";
        Files.writeString(agentHome.resolve("soul.md"), customSoulContent);

        initializer.initHome("test-agent", agentHome);

        assertTrue(Files.exists(agentHome.resolve("CLAUDE.md")));
        assertEquals(customSoulContent, Files.readString(agentHome.resolve("soul.md")));
        assertTrue(Files.exists(agentHome.resolve("owner.md")));
        assertTrue(Files.exists(agentHome.resolve("tool.md")));
        assertTrue(Files.exists(agentHome.resolve("memory").resolve("overview.md")));
    }
}
