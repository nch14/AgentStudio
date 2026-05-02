package com.chenhaonee.agents.claudecode.workspace;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.mcp.McpConfigGenerator;
import com.chenhaonee.agents.claudecode.prompt.ClaudeCodePrompts;
import com.chenhaonee.agents.connect.spi.core.AgentHomeInitializer;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Claude Code 的 Agent Home 初始化实现。
 * 负责在 agent 创建时初始化 home 目录下的基础文件与 mcp-config.json。
 */
@Component
@RequiredArgsConstructor
public class ClaudeCodeHomeInitializer implements AgentHomeInitializer {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeHomeInitializer.class);
    private static final String MEMORY_DIR = "memory";
    private static final String MEMORY_DAILY_DIR = "daily";
    private static final String MEMORY_TOPICS_DIR = "topics";
    private static final String MEMORY_OVERVIEW_FILE = "overview.md";

    private final ClaudeCodeProperties properties;
    private final ClaudeCodePrompts claudeCodePrompts;
    private final McpConfigGenerator mcpConfigGenerator;

    @Override
    public AgentProvider supportedType() {
        return AgentProvider.CLAUDE_CODE;
    }

    @Override
    public void initHome(String agentCode, Path agentHome) throws IOException {
        initializeMarkdownFile(agentHome.resolve("CLAUDE.md"), claudeCodePrompts.renderClaudeMdChat(agentCode),
                "CLAUDE.md", agentCode);
        initializeMarkdownFile(agentHome.resolve("soul.md"), claudeCodePrompts.renderSoulMd(agentCode),
                "soul.md", agentCode);
        initializeMarkdownFile(agentHome.resolve("owner.md"), claudeCodePrompts.renderOwnerMd(agentCode),
                "owner.md", agentCode);
        initializeMarkdownFile(agentHome.resolve("tool.md"), claudeCodePrompts.renderToolMd(),
                "tool.md", agentCode);
        initializeMemoryDirectories(agentHome, agentCode);
        initializeMarkdownFile(agentHome.resolve(MEMORY_DIR).resolve(MEMORY_OVERVIEW_FILE),
                claudeCodePrompts.renderMemoryOverviewMd(), "memory/overview.md", agentCode);

        Path mcpFile = mcpConfigGenerator.generateIfAbsent(agentHome, properties.getMcpServerUrl());
        log.info("Ensured mcp-config.json for agent {}: {}", agentCode, mcpFile);
    }

    private void initializeMarkdownFile(Path file, String content, String fileName, String agentCode) throws IOException {
        if (Files.exists(file)) {
            return;
        }
        Files.writeString(file, content);
        log.info("Created {} for agent: {}", fileName, agentCode);
    }

    private void initializeMemoryDirectories(Path agentHome, String agentCode) throws IOException {
        Path dailyDir = agentHome.resolve(MEMORY_DIR).resolve(MEMORY_DAILY_DIR);
        Path topicsDir = agentHome.resolve(MEMORY_DIR).resolve(MEMORY_TOPICS_DIR);
        Files.createDirectories(dailyDir);
        Files.createDirectories(topicsDir);
        log.info("Ensured memory directories for agent: {}", agentCode);
    }
}
