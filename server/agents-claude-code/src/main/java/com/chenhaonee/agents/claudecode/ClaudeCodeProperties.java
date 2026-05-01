package com.chenhaonee.agents.claudecode;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Claude Code CLI 配置属性。
 */
@Configuration
@ConfigurationProperties(prefix = "agents.claude-code")
public class ClaudeCodeProperties {

    /** CLI 可执行路径 */
    private String cliPath = "claude";

    /** 默认模型 */
    private String defaultModel = "claude-sonnet-4-20250514";

    /** chat 模式超时（秒） */
    private int chatTimeoutSeconds = 300;

    /** task 模式超时（秒） */
    private int taskTimeoutSeconds = 1800;

    /** 最大并发进程数（task 模式） */
    private int maxConcurrentProcesses = 5;

    /** chat session 进程 LRU 池上限 */
    private int maxChatProcesses = 5;

    /** chat session 进程空闲超时（秒），超时后被 LRU 驱逐并回收 */
    private int chatIdleTimeoutSeconds = 1800;

    /** task 内部最大轮次 */
    private int taskMaxTurns = 50;

    /** 主应用 MCP Server 地址 */
    private String mcpServerUrl = "http://localhost:8080/sse";

    /** CLI 权限模式 */
    private String permissionMode = "bypassPermissions";

    /** 任务目录保留天数 */
    private int taskRetentionDays = 7;

    public String getCliPath() {
        return cliPath;
    }

    public void setCliPath(String cliPath) {
        this.cliPath = cliPath;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public int getChatTimeoutSeconds() {
        return chatTimeoutSeconds;
    }

    public void setChatTimeoutSeconds(int chatTimeoutSeconds) {
        this.chatTimeoutSeconds = chatTimeoutSeconds;
    }

    public int getTaskTimeoutSeconds() {
        return taskTimeoutSeconds;
    }

    public void setTaskTimeoutSeconds(int taskTimeoutSeconds) {
        this.taskTimeoutSeconds = taskTimeoutSeconds;
    }

    public int getMaxConcurrentProcesses() {
        return maxConcurrentProcesses;
    }

    public void setMaxConcurrentProcesses(int maxConcurrentProcesses) {
        this.maxConcurrentProcesses = maxConcurrentProcesses;
    }

    public int getMaxChatProcesses() {
        return maxChatProcesses;
    }

    public void setMaxChatProcesses(int maxChatProcesses) {
        this.maxChatProcesses = maxChatProcesses;
    }

    public int getChatIdleTimeoutSeconds() {
        return chatIdleTimeoutSeconds;
    }

    public void setChatIdleTimeoutSeconds(int chatIdleTimeoutSeconds) {
        this.chatIdleTimeoutSeconds = chatIdleTimeoutSeconds;
    }

    public int getTaskMaxTurns() {
        return taskMaxTurns;
    }

    public void setTaskMaxTurns(int taskMaxTurns) {
        this.taskMaxTurns = taskMaxTurns;
    }

    public String getMcpServerUrl() {
        return mcpServerUrl;
    }

    public void setMcpServerUrl(String mcpServerUrl) {
        this.mcpServerUrl = mcpServerUrl;
    }

    public String getPermissionMode() {
        if ("bypass".equals(permissionMode)) {
            return "bypassPermissions";
        }
        return permissionMode;
    }

    public void setPermissionMode(String permissionMode) {
        this.permissionMode = permissionMode;
    }

    public int getTaskRetentionDays() {
        return taskRetentionDays;
    }

    public void setTaskRetentionDays(int taskRetentionDays) {
        this.taskRetentionDays = taskRetentionDays;
    }
}
