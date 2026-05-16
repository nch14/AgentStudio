package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.agent.ProviderCapabilityService;
import com.chenhaonee.agents.common.domain.Identity;
import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.connect.support.AgentMessageBlockPayloads;
import com.chenhaonee.agents.connect.support.MessagesEventBlockRecorderFactory;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import com.chenhaonee.agents.domain.session.factory.AgentSessionDomainFactory;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.ContentBlockType;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import com.chenhaonee.agents.domain.session.repository.AgentSessionRepository;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * Anthropic Messages 协议入口的应用服务。
 *
 * <p>把 provider 流式输出经 {@link CancellableStream} 桥接到下游 SSE，
 * 同时由 {@link MessagesEventBlockRecorderFactory} 按 block 边界实时落库；
 * 活跃流注册到 {@link ActiveStreamRegistry} 以支持断线重订阅与主动中断。</p>
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
    private final MessagesEventBlockRecorderFactory messagesEventBlockRecorderFactory;
    private final ActiveStreamRegistry activeStreamRegistry;

    public AnthropicMessagesResult create(String agentCode, String sessionCode, String requestJson) {
        Agent agent = agentDomainService.requireEnabledAgent(agentCode);
        validateProvider(agent.getProvider());
        validateRequest(requestJson);
        String latestUserText = extractLatestUserText(requestJson);
        AgentSession session = getOrCreateSession(sessionCode, agent.getCode(), latestUserText);
        String turnCode = Identity.newIdentity().value();
        MessageProtocolType protocolType = mapProtocol(agent.getProvider());
        persistLatestUserTextBlock(session.getCode(), turnCode, latestUserText, protocolType);

        AgentProvider providerType = agent.getProvider();
        MessagesAgent messagesAgent = agentRegistry.findMessagesAgent(providerType)
                .orElseThrow(() -> new IllegalStateException("messages agent not found for provider: " + providerType));

        Flux<MessagesEvent> raw = messagesAgent.stream(agentCode, requestJson, session.getCode());
        CancellableStream bridge = new CancellableStream(raw);
        Flux<MessagesEvent> shared = bridge.asFlux();
        MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder =
                messagesEventBlockRecorderFactory.create(session.getCode(), turnCode, protocolType);
        boolean streaming = isStreamRequest(requestJson);

        if (streaming) {
            // cancelAction：先 downstream 注入 interrupted，再 upstream kill 真正终止 agent 思考/工具调用
            Runnable cancelAction = () -> {
                bridge.cancel();
                messagesAgent.cancelStream(session.getCode());
            };
            Flux<MessagesEvent> standardEvents = shared.filter(this::isStandardStreamingEvent);
            activeStreamRegistry.register(session.getCode(), standardEvents, cancelAction);
            subscribeRecorder(session.getCode(), shared, recorder, () -> activeStreamRegistry.remove(session.getCode()));
            bridge.connect();
            Flux<ServerSentEvent<String>> events = standardEvents.map(this::toServerSentEvent);
            return AnthropicMessagesResult.streaming(session.getCode(), SseKeepAlive.wrap(events));
        }

        CompletableFuture<List<MessagesEvent>> eventsFuture = shared.collectList().toFuture();
        subscribeRecorder(session.getCode(), shared, recorder, () -> {});
        bridge.connect();
        List<MessagesEvent> events = eventsFuture.join();
        String messageJson = buildAnthropicMessageJson(events);
        return AnthropicMessagesResult.nonStreaming(session.getCode(), messageJson);
    }

    public boolean isStreamRequest(String requestJson) {
        JSONObject root = JSON.parseObject(requestJson);
        return root != null && Boolean.TRUE.equals(root.getBoolean("stream"));
    }

    private void subscribeRecorder(
            String sessionCode,
            Flux<MessagesEvent> shared,
            MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder,
            Runnable cleanup
    ) {
        shared.doFinally(signal -> cleanup.run())
                .subscribe(
                        event -> safeRecordBlock(sessionCode, recorder, event),
                        error -> log.error("messages stream failed for session={}", sessionCode, error)
                );
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

    private void persistLatestUserTextBlock(
            String sessionCode,
            String turnCode,
            String latestUserText,
            MessageProtocolType protocolType
    ) {
        if (StringUtils.isBlank(latestUserText)) {
            return;
        }
        agentSessionDomainService.appendBlock(
                sessionCode,
                turnCode,
                MessageRole.USER,
                ContentBlockType.TEXT,
                protocolType,
                AgentMessageBlockPayloads.text(latestUserText),
                null
        );
    }

    private MessageProtocolType mapProtocol(AgentProvider provider) {
        return switch (provider) {
            case CLAUDE_CODE -> MessageProtocolType.ANTHROPIC_MESSAGES;
        };
    }

    private boolean isStandardStreamingEvent(MessagesEvent event) {
        if (event == null || StringUtils.isBlank(event.eventType())) {
            return false;
        }
        return switch (event.eventType()) {
            case "turn_start",
                 "message_start",
                 "content_block_start",
                 "content_block_delta",
                 "content_block_stop",
                 "message_delta",
                 "message_stop",
                 "turn_stop",
                 "error",
                 "ping",
                 "interrupted" -> true;
            default -> false;
        };
    }

    private ServerSentEvent<String> toServerSentEvent(MessagesEvent event) {
        return ServerSentEvent.<String>builder()
                .event(event.eventType())
                .data(event.dataJson())
                .build();
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

    private void safeRecordBlock(
            String sessionCode,
            MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder,
            MessagesEvent event
    ) {
        try {
            recorder.onEvent(event);
        } catch (Exception e) {
            log.error("failed to persist message block for session={}", sessionCode, e);
        }
    }

    private String buildAnthropicMessageJson(List<MessagesEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }
        JSONObject finalMessage = null;
        Map<Integer, StringBuilder> toolInputBuffers = new HashMap<>();
        for (MessagesEvent event : events) {
            if (!isStandardStreamingEvent(event) || StringUtils.isBlank(event.dataJson())) {
                continue;
            }
            JSONObject data = JSON.parseObject(event.dataJson());
            if (data == null) {
                continue;
            }
            switch (event.eventType()) {
                case "message_start" -> {
                    JSONObject message = data.getJSONObject("message");
                    if (message != null) {
                        finalMessage = JSON.parseObject(JSON.toJSONString(message));
                        if (finalMessage.getJSONArray("content") == null) {
                            finalMessage.put("content", new JSONArray());
                        }
                    }
                }
                case "content_block_start" -> {
                    if (finalMessage == null) {
                        continue;
                    }
                    JSONObject contentBlock = data.getJSONObject("content_block");
                    Integer index = data.getInteger("index");
                    if (contentBlock == null || index == null) {
                        continue;
                    }
                    JSONArray content = finalMessage.getJSONArray("content");
                    ensureArraySize(content, index);
                    content.set(index, JSON.parseObject(JSON.toJSONString(contentBlock)));
                }
                case "content_block_delta" -> {
                    if (finalMessage == null) {
                        continue;
                    }
                    JSONObject delta = data.getJSONObject("delta");
                    Integer index = data.getInteger("index");
                    if (delta == null || index == null) {
                        continue;
                    }
                    JSONObject block = finalMessage.getJSONArray("content").getJSONObject(index);
                    if (block == null) {
                        continue;
                    }
                    switch (delta.getString("type")) {
                        case "text_delta" -> block.put("text", append(block.getString("text"), delta.getString("text")));
                        case "thinking_delta" ->
                                block.put("thinking", append(block.getString("thinking"), delta.getString("thinking")));
                        case "signature_delta" -> block.put("signature", append(block.getString("signature"), delta.getString("signature")));
                        case "input_json_delta" -> toolInputBuffers
                                .computeIfAbsent(index, ignored -> new StringBuilder())
                                .append(delta.getString("partial_json"));
                        default -> {
                        }
                    }
                }
                case "content_block_stop" -> {
                    if (finalMessage == null) {
                        continue;
                    }
                    Integer index = data.getInteger("index");
                    if (index == null) {
                        continue;
                    }
                    StringBuilder inputBuffer = toolInputBuffers.remove(index);
                    if (inputBuffer == null || inputBuffer.isEmpty()) {
                        continue;
                    }
                    JSONObject block = finalMessage.getJSONArray("content").getJSONObject(index);
                    if (block != null) {
                        block.put("input", AgentMessageBlockPayloads.parseJsonObjectOrRaw(inputBuffer.toString()));
                    }
                }
                case "message_delta" -> {
                    if (finalMessage == null) {
                        continue;
                    }
                    JSONObject delta = data.getJSONObject("delta");
                    if (delta != null) {
                        for (Map.Entry<String, Object> entry : delta.entrySet()) {
                            finalMessage.put(entry.getKey(), entry.getValue());
                        }
                    }
                    JSONObject usage = data.getJSONObject("usage");
                    if (usage != null) {
                        finalMessage.put("usage", usage);
                    }
                }
                default -> {
                }
            }
        }
        return finalMessage == null ? null : JSON.toJSONString(finalMessage);
    }

    private void ensureArraySize(JSONArray content, int index) {
        while (content.size() <= index) {
            content.add(null);
        }
    }

    private String append(String current, String delta) {
        return StringUtils.defaultString(current) + StringUtils.defaultString(delta);
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
