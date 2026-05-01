package com.chenhaonee.agents.claudecode.configkey;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.connect.spi.core.AgentProviderConfigDescriptor;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Claude Code Agent 声明的 providerConfig 配置项。
 */
@Component
public class ClaudeCodeProviderConfigDescriptor implements AgentProviderConfigDescriptor {

    private final ClaudeCodeProperties properties;

    public ClaudeCodeProviderConfigDescriptor(ClaudeCodeProperties properties) {
        this.properties = properties;
    }

    @Override
    public AgentProvider supportedType() {
        return AgentProvider.CLAUDE_CODE;
    }

    @Override
    public List<ConfigKeyDescriptor> supportedConfigKeys() {
        return List.of(
                new ConfigKeyDescriptor("model", "模型", "使用的 Claude 模型标识", false, properties.getDefaultModel()),
                new ConfigKeyDescriptor("maxTurns", "单次执行最大内部轮次", "Claude Code 内部最大对话轮次", false, String.valueOf(properties.getTaskMaxTurns())),
                new ConfigKeyDescriptor("permissionMode", "权限模式", "Claude Code CLI 权限模式", false, properties.getPermissionMode())
        );
    }
}
