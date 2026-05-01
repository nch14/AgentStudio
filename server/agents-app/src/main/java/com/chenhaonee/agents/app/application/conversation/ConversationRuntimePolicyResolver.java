package com.chenhaonee.agents.app.application.conversation;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.domain.agent.model.Agent;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ConversationRuntimePolicyResolver {

    private static final int DEFAULT_ANTHROPIC_MAX_TOKENS = 8192;

    private final ClaudeCodeProperties claudeCodeProperties;

    public ConversationRuntimePolicy resolve(Agent agent) {
        Map<String, String> providerConfig = agent.getProviderConfig();
        return resolveAnthropicPolicy(providerConfig);
    }

    private ConversationRuntimePolicy resolveAnthropicPolicy(Map<String, String> providerConfig) {
        String model = StringUtils.firstNonBlank(valueOf(providerConfig, "model"), claudeCodeProperties.getDefaultModel());
        return ConversationRuntimePolicy.anthropicMessages(model, DEFAULT_ANTHROPIC_MAX_TOKENS, null);
    }

    private String valueOf(Map<String, String> providerConfig, String key) {
        if (providerConfig == null || providerConfig.isEmpty()) {
            return null;
        }
        return providerConfig.get(key);
    }
}
