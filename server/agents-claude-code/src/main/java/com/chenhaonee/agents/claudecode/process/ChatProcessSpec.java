package com.chenhaonee.agents.claudecode.process;

/**
 * Chat 进程的静态启动参数。
 */
public record ChatProcessSpec(
        String workDir,
        String mcpConfigPath,
        String model,
        String systemPrompt
) {
}
