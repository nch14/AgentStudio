package com.chenhaonee.agents.app.application.conversation;

public record ConversationRuntimePolicy(
        String protocolType,
        String model,
        Integer maxTokens,
        Double temperature,
        String systemPrompt
) {

    public static final String ANTHROPIC_MESSAGES = "ANTHROPIC_MESSAGES";

    public static ConversationRuntimePolicy anthropicMessages(String model, Integer maxTokens, String systemPrompt) {
        return new ConversationRuntimePolicy(ANTHROPIC_MESSAGES, model, maxTokens, null, systemPrompt);
    }

    public boolean isAnthropicMessages() {
        return ANTHROPIC_MESSAGES.equals(protocolType);
    }
}
