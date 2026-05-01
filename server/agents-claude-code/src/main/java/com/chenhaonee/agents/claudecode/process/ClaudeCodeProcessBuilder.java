package com.chenhaonee.agents.claudecode.process;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 封装 Claude Code CLI 命令行参数组装。
 */
@Component
@RequiredArgsConstructor
public class ClaudeCodeProcessBuilder {

    private final ClaudeCodeProperties properties;

    /**
     * 构建 chat 模式的进程命令。
     * 包含 {@code --input-format stream-json}，stdin 常开，支持多轮持久化。
     *
     * @param isResume  true 表示续接已有 session（使用 --resume），false 表示创建新 session（使用 --session-id）
     * @param sessionId 新 session 时为 Java 侧生成的 UUID；续接时为 Claude Code 返回的真实 session_id
     */
    public List<String> buildChatProcess(String workDir, boolean isResume, String sessionId, String model, String mcpConfigPath, String systemPrompt) {
        List<String> command = buildBaseCommand(model, mcpConfigPath);
        // chat 专用：stream-json 输入协议，stdin 常开支持多轮
        command.add("--input-format");
        command.add("stream-json");
        command.add("--include-partial-messages");

        if (sessionId != null && !sessionId.isBlank()) {
            if (isResume) {
                command.add("--resume");
            } else {
                command.add("--session-id");
            }
            command.add(sessionId);
        }

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            command.add("--system-prompt");
            command.add(systemPrompt);
        }

        return command;
    }

    /**
     * 构建 task 模式的进程命令。
     *
     * @param isResume  true 表示续接已有 session（使用 --resume），false 表示新 session（不追加 session 参数）
     * @param sessionId 续接时为 Claude Code 真实 session_id；isResume=false 时可为 null
     */
    public List<String> buildTaskProcess(String workDir, boolean isResume, String sessionId, String model, int maxTurns, String mcpConfigPath, String systemPrompt) {
        List<String> command = buildBaseCommand(model, mcpConfigPath);

        if (isResume && sessionId != null && !sessionId.isBlank()) {
            command.add("--resume");
            command.add(sessionId);
        }

        command.add("--max-turns");
        command.add(String.valueOf(maxTurns));

        if (systemPrompt != null && !systemPrompt.isBlank()) {
            command.add("--system-prompt");
            command.add(systemPrompt);
        }

        return command;
    }

    private List<String> buildBaseCommand(String model, String mcpConfigPath) {
        List<String> command = new ArrayList<>();
        command.add(properties.getCliPath());
        command.add("--print");
        command.add("--output-format");
        command.add("stream-json");
        command.add("--verbose");
        command.add("--model");
        command.add(model);

        if (mcpConfigPath != null && !mcpConfigPath.isBlank()) {
            command.add("--mcp-config");
            command.add(mcpConfigPath);
        }

        command.add("--permission-mode");
        command.add(properties.getPermissionMode());

        return command;
    }
}
