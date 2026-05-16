package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.agent.ProviderCapabilityService;
import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import com.chenhaonee.agents.domain.session.factory.AgentSessionDomainFactory;
import com.chenhaonee.agents.domain.session.model.AgentMessage;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import com.chenhaonee.agents.domain.session.repository.AgentSessionRepository;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;

/**
 * Anthropic Messages 应用服务。
 */
@Service
@RequiredArgsConstructor
public class AnthropicMessagesService {

    private static final Logger log = LoggerFactory.getLogger(AnthropicMessagesService.class);

    private final AgentSessionRepository agentSessionRepository;
    private final AgentSessionDomainService agentSessionDomainService;
    private final AgentSessionDomainFactory agentSessionDomainFactory;
    private final AgentDomainService agentDomainService;
    private final AgentRegistry agentRegistry;
    private final ProviderCapabilityService providerCapabilityService;

    public AnthropicMessagesResult create(String agentCode, String sessionCode, String requestJson) {
        Agent agent = agentDomainService.requireEnabledAgent(agentCode);
        validateProvider(agent.getProvider());
        validateRequest(requestJson);
        AgentSession session = getOrCreateSession(sessionCode, agent.getCode(), extractLatestUserText(requestJson));
        persistLatestUserMessage(session.getCode(), requestJson);

        AgentProvider providerType = agent.getProvider();
        MessagesAgent messagesAgent = agentRegistry.findMessagesAgent(providerType)
                .orElseThrow(() -> new IllegalStateException("messages agent not found for provider: " + providerType));

        AgentMessage assistantMessage = createAssistantPlaceholder(session.getCode());
        AnthropicMessageAssembler assembler = new AnthropicMessageAssembler();

        Flux<MessagesEvent> result = messagesAgent.stream(agentCode, requestJson, session.getCode());
        ConnectableFlux<MessagesEvent> shared = result.replay();
        shared.subscribe(
                assembler::onEvent,
                error -> persistAssistantFailure(session.getCode(), assistantMessage.getCode(), assembler, error),
                () -> persistAssistantCompletion(session.getCode(), assistantMessage.getCode(), assembler)
        );
        shared.connect();

        if (isStreamRequest(requestJson)) {
            Flux<ServerSentEvent<String>> events = shared.map(this::toServerSentEvent);
            return AnthropicMessagesResult.streaming(session.getCode(), events);
        }

        String messageJson = shared.ignoreElements()
                .thenReturn(assembler.toFinalMessageJson())
                .block();
        return AnthropicMessagesResult.nonStreaming(session.getCode(), messageJson);
    }

    public boolean isStreamRequest(String requestJson) {
        JSONObject root = JSON.parseObject(requestJson);
        return root != null && Boolean.TRUE.equals(root.getBoolean("stream"));
    }

    private void validateProvider(AgentProvider providerType) {
        if (!providerCapabilityService.supportsMessages(providerType)) {
            throw new IllegalArgumentException("provider " + providerType + " does not support messages");
        }
    }

    private void validateRequest(String requestJson) {
        JSONObject root = JSON.parseObject(requestJson);
        if (root == null) {
            throw new IllegalArgumentException("anthropic messages request must be valid json");
        }
        if (StringUtils.isBlank(root.getString("model"))) {
            throw new IllegalArgumentException("anthropic messages request must contain model");
        }
        Integer maxTokens = root.getInteger("max_tokens");
        if (maxTokens == null || maxTokens <= 0) {
            throw new IllegalArgumentException("anthropic messages request must contain positive max_tokens");
        }
        if (StringUtils.isBlank(extractLatestUserText(root))) {
            throw new IllegalArgumentException("anthropic messages request must contain supported user content");
        }
    }

    private AgentSession getOrCreateSession(String sessionCode, String agentCode, String userText) {
        if (StringUtils.isNotBlank(sessionCode)) {
            return agentSessionRepository.findByCodeAndAgentCode(sessionCode, agentCode)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "session not found for agent: sessionCode=" + sessionCode + ", agentCode=" + agentCode));
        }
        AgentSession session = agentSessionDomainFactory.create(
                StringUtils.left(StringUtils.defaultString(userText), 100), agentCode, SessionScene.CHAT);
        return agentSessionRepository.save(session);
    }

    private void persistLatestUserMessage(String sessionCode, String requestJson) {
        JSONObject latestUserMessage = extractLatestUserMessage(requestJson);
        if (latestUserMessage == null) {
            throw new IllegalArgumentException("anthropic messages request must contain a user message");
        }
        AgentMessage message = AgentMessage.anthropicUserMessage(sessionCode, JSON.toJSONString(latestUserMessage));
        agentSessionDomainService.appendMessage(sessionCode, message);
    }

    private AgentMessage createAssistantPlaceholder(String sessionCode) {
        return agentSessionDomainService.appendMessageAndReturnMessage(
                sessionCode, AgentMessage.anthropicAssistantPlaceholder(sessionCode));
    }

    private void persistAssistantCompletion(String sessionCode, String messageCode, AnthropicMessageAssembler assembler) {
        try {
            agentSessionDomainService.completeMessage(messageCode, assembler.toFinalMessageJson(), extractExternalMessageId(assembler));
        } catch (Exception e) {
            log.error("failed to persist anthropic assistant reply for session={}", sessionCode, e);
        }
    }

    private void persistAssistantFailure(
            String sessionCode,
            String messageCode,
            AnthropicMessageAssembler assembler,
            Throwable error
    ) {
        try {
            agentSessionDomainService.failMessage(
                    messageCode,
                    assembler.toFinalMessageJson(),
                    JSON.toJSONString(buildErrorBody(error.getMessage() == null ? "unknown error" : error.getMessage()))
            );
        } catch (Exception persistError) {
            log.error("failed to persist anthropic assistant error for session={}", sessionCode, persistError);
            return;
        }
        log.error("anthropic messages stream failed for session={}", sessionCode, error);
    }

    private String extractExternalMessageId(AnthropicMessageAssembler assembler) {
        String finalMessageJson = assembler.toFinalMessageJson();
        if (StringUtils.isBlank(finalMessageJson)) {
            return null;
        }
        JSONObject message = JSON.parseObject(finalMessageJson);
        return message == null ? null : StringUtils.trimToNull(message.getString("id"));
    }

    private ServerSentEvent<String> toServerSentEvent(MessagesEvent event) {
        return ServerSentEvent.<String>builder()
                .event(event.eventType())
                .data(event.dataJson())
                .build();
    }

    private JSONObject extractLatestUserMessage(String requestJson) {
        return extractLatestUserMessage(JSON.parseObject(requestJson));
    }

    private JSONObject extractLatestUserMessage(JSONObject root) {
        if (root == null) {
            return null;
        }
        JSONArray messages = root.getJSONArray("messages");
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            JSONObject message = messages.getJSONObject(i);
            if (message != null && "user".equals(message.getString("role"))) {
                return message;
            }
        }
        return null;
    }

    private String extractLatestUserText(String requestJson) {
        return extractLatestUserText(JSON.parseObject(requestJson));
    }

    private String extractLatestUserText(JSONObject root) {
        JSONObject latestUserMessage = extractLatestUserMessage(root);
        if (latestUserMessage == null) {
            return null;
        }
        Object content = latestUserMessage.get("content");
        if (content instanceof String text) {
            return StringUtils.trimToNull(text);
        }
        if (content instanceof JSONArray blocks) {
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < blocks.size(); i++) {
                JSONObject block = blocks.getJSONObject(i);
                if (block == null) {
                    continue;
                }
                if ("text".equals(block.getString("type")) && StringUtils.isNotBlank(block.getString("text"))) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(block.getString("text"));
                    continue;
                }
                if ("tool_result".equals(block.getString("type")) && StringUtils.isNotBlank(block.getString("content"))) {
                    if (!builder.isEmpty()) {
                        builder.append('\n');
                    }
                    builder.append(block.getString("content"));
                    continue;
                }
                throw new IllegalArgumentException("anthropic messages currently only support text and tool_result user content blocks");
            }
            return StringUtils.trimToNull(builder.toString());
        }
        return null;
    }

    private JSONObject buildErrorBody(String message) {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        JSONObject detail = new JSONObject();
        detail.put("type", "api_error");
        detail.put("message", message);
        error.put("error", detail);
        return error;
    }

    public static final class AnthropicMessagesResult {
        private final String sessionCode;
        private final String messageJson;
        private final Flux<ServerSentEvent<String>> events;

        private AnthropicMessagesResult(String sessionCode, String messageJson, Flux<ServerSentEvent<String>> events) {
            this.sessionCode = sessionCode;
            this.messageJson = messageJson;
            this.events = events;
        }

        public String sessionCode() {
            return sessionCode;
        }

        public String messageJson() {
            return messageJson;
        }

        public Flux<ServerSentEvent<String>> events() {
            return events;
        }

        public static AnthropicMessagesResult nonStreaming(String sessionCode, String messageJson) {
            return new AnthropicMessagesResult(sessionCode, messageJson, null);
        }

        public static AnthropicMessagesResult streaming(String sessionCode, Flux<ServerSentEvent<String>> events) {
            return new AnthropicMessagesResult(sessionCode, null, events);
        }
    }
}
