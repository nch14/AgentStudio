package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService.AnthropicMessagesResult;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@RequiredArgsConstructor
public class AgentConversationFacade {

    private final AgentDomainService agentDomainService;
    private final ConversationRuntimePolicyResolver conversationRuntimePolicyResolver;
    private final AnthropicMessagesService anthropicMessagesService;

    public StreamingConversationResult sendMessage(String agentCode, String sessionCode, String content) {
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("content must not be blank");
        }
        Agent agent = agentDomainService.requireEnabledAgent(agentCode);
        ConversationRuntimePolicy runtimePolicy = conversationRuntimePolicyResolver.resolve(agent);
        String requestJson = buildRequestJson(runtimePolicy, content);
        AnthropicMessagesResult result = anthropicMessagesService.create(agentCode, sessionCode, requestJson);
        return new StreamingConversationResult(
                result.sessionCode(),
                runtimePolicy.protocolType(),
                requireStreamingEvents(result.events())
        );
    }

    private String buildRequestJson(ConversationRuntimePolicy runtimePolicy, String content) {
        JSONObject root = new JSONObject();
        root.put("model", runtimePolicy.model());
        root.put("stream", true);
        root.put("max_tokens", runtimePolicy.maxTokens());
        if (StringUtils.isNotBlank(runtimePolicy.systemPrompt())) {
            root.put("system", runtimePolicy.systemPrompt());
        }
        root.put("messages", buildAnthropicMessages(content));
        return JSON.toJSONString(root);
    }

    private JSONArray buildAnthropicMessages(String content) {
        JSONArray messages = new JSONArray();
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", content);
        messages.add(userMessage);
        return messages;
    }

    private Flux<ServerSentEvent<String>> requireStreamingEvents(Flux<ServerSentEvent<String>> events) {
        if (events == null) {
            throw new IllegalStateException("streaming events missing");
        }
        return events;
    }

    public record StreamingConversationResult(
            String sessionCode,
            String protocolType,
            Flux<ServerSentEvent<String>> events
    ) {
    }
}
