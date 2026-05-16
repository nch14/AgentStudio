package com.chenhaonee.agents.claudecode.agents;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.process.ChatProcessSpec;
import com.chenhaonee.agents.claudecode.process.ChatSessionRegistry;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcess;
import com.chenhaonee.agents.claudecode.process.SessionBusyException;
import com.chenhaonee.agents.claudecode.process.TooManyActiveChatsException;
import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;
import com.chenhaonee.agents.claudecode.stream.StreamJsonToMessagesEventMapper;
import com.chenhaonee.agents.claudecode.workspace.WorkspaceManager;
import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.capability.SessionApi;
import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.ConnectableFlux;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 基于 Claude Code CLI 的 MessagesAgent 实现。
 */
@Component
@RequiredArgsConstructor
public class ClaudeCodeMessagesAgent implements MessagesAgent {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeMessagesAgent.class);

    private final ClaudeCodeProperties properties;
    private final AgentConfigApi agentConfigApi;
    private final WorkspaceManager workspaceManager;
    private final ChatSessionRegistry sessionRegistry;
    private final SessionApi sessionApi;

    private final StreamJsonToMessagesEventMapper messagesEventMapper = new StreamJsonToMessagesEventMapper();

    private record MessagesConversation(Mono<String> resolvedSessionId, Flux<MessagesEvent> events) {
    }

    private record MessagesRequestContext(String model, String systemPrompt, String userInput) {
    }

    @Override
    public AgentProvider supportedType() {
        return AgentProvider.CLAUDE_CODE;
    }

    @Override
    public boolean cancelStream(String sessionCode) {
        boolean interrupted = sessionRegistry.interruptActiveTurn(sessionCode);
        if (interrupted) {
            log.info("Claude messages turn interrupted for sessionCode={}", sessionCode);
        }
        return interrupted;
    }

    @Override
    public Flux<MessagesEvent> stream(String agentCode, String requestJson, String sessionCode) {
        Map<String, String> providerConfig = agentConfigApi.getProviderConfig(agentCode);
        String providerSessionId = StringUtils.trimToNull(sessionApi.getProviderSessionId(
                SessionRelationTargetType.AGENT_SESSION, sessionCode, AgentProvider.CLAUDE_CODE));
        MessagesRequestContext requestContext = parseRequestContext(requestJson, providerConfig);

        try {
            workspaceManager.prepareHome(agentCode);
            MessagesConversation conversation = startConversation(
                    agentCode,
                    requestContext,
                    sessionCode,
                    providerSessionId,
                    true
            );
            conversation.resolvedSessionId()
                    .filter(StringUtils::isNotBlank)
                    .doOnNext(resolvedId -> syncProviderSession(sessionCode, resolvedId))
                    .doOnError(e -> log.error("failed to bind Claude provider session for sessionCode={}", sessionCode, e))
                    .subscribe();
            return conversation.events();
        } catch (TooManyActiveChatsException e) {
            log.warn("Too many active chats, rejecting sessionCode={}", sessionCode);
            return errorEvents("当前活跃会话过多，请稍后再试。");
        } catch (SessionBusyException e) {
            log.warn("Session busy: {}", sessionCode);
            return errorEvents("当前会话还有未完成的请求，请稍候。");
        } catch (Exception e) {
            log.error("messages startup failed agent={}, sessionCode={}", agentCode, sessionCode, e);
            return errorEvents("系统错误：无法启动 Agent，请稍后重试。");
        }
    }

    private MessagesConversation startConversation(
            String agentCode,
            MessagesRequestContext requestContext,
            String sessionCode,
            String providerSessionId,
            boolean allowMissingSessionRetry
    ) {
        Path agentHome = workspaceManager.getAgentHome(agentCode);
        Path mcpFile = agentHome.resolve("mcp-config.json");
        ChatProcessSpec spec = new ChatProcessSpec(
                agentHome.toString(),
                mcpFile.toString(),
                requestContext.model(),
                requestContext.systemPrompt()
        );
        ClaudeCodeProcess process = sessionRegistry.acquire(sessionCode, spec, providerSessionId);
        Flux<StreamJsonEvent> turnEvents;
        try {
            turnEvents = process.startTurn(requestContext.userInput());
        } catch (Exception e) {
            sessionRegistry.release(sessionCode);
            throw new RuntimeException("Failed to start Claude messages turn for sessionCode=" + sessionCode, e);
        }

        ConnectableFlux<StreamJsonEvent> shared = turnEvents.replay(256);

        Mono<Boolean> missingSessionMono = shared.next()
                .map(this::isMissingSessionError)
                .defaultIfEmpty(false)
                .cache();

        Mono<String> currentResolvedSessionId = shared
                .filter(this::hasProviderSessionId)
                .map(StreamJsonEvent::session_id)
                .filter(StringUtils::isNotBlank)
                .next()
                .switchIfEmpty(Mono.justOrEmpty(providerSessionId))
                .doOnNext(id -> sessionRegistry.bindProviderSessionId(sessionCode, id))
                .cache();

        Mono<MessagesConversation> retryResultMono = missingSessionMono
                .filter(Boolean.TRUE::equals)
                .flatMap(ignored -> retryWithFreshSession(agentCode, requestContext, sessionCode, allowMissingSessionRetry))
                .cache();

        Mono<String> resolvedSessionId = missingSessionMono.flatMap(missingSession -> {
            if (Boolean.TRUE.equals(missingSession)) {
                return retryResultMono.flatMap(MessagesConversation::resolvedSessionId);
            }
            return currentResolvedSessionId;
        });

        Flux<MessagesEvent> events = missingSessionMono.flatMapMany(missingSession -> {
            if (Boolean.TRUE.equals(missingSession)) {
                return retryResultMono.flatMapMany(MessagesConversation::events);
            }
            return messagesEventMapper.project(shared)
                    .doFinally(signal -> sessionRegistry.release(sessionCode));
        });

        shared.connect();
        return new MessagesConversation(resolvedSessionId, events);
    }

    private Mono<MessagesConversation> retryWithFreshSession(
            String agentCode,
            MessagesRequestContext requestContext,
            String sessionCode,
            boolean allowMissingSessionRetry
    ) {
        if (!allowMissingSessionRetry) {
            return Mono.just(new MessagesConversation(Mono.empty(), errorEvents("系统错误：Claude 会话恢复失败，请稍后重试。")));
        }

        releaseAndEvictStaleSession(sessionCode);
        log.warn("Stale Claude provider session detected, restarting fresh messages session. sessionCode={}", sessionCode);
        return Mono.fromSupplier(() -> startConversation(agentCode, requestContext, sessionCode, null, false));
    }

    private MessagesRequestContext parseRequestContext(String requestJson, Map<String, String> providerConfig) {
        JSONObject root = JSON.parseObject(requestJson);
        String model = StringUtils.firstNonBlank(root.getString("model"), providerConfig.get("model"), properties.getDefaultModel());
        String systemPrompt = extractSystemPrompt(root.get("system"));
        String userInput = extractLatestUserText(root.getJSONArray("messages"));
        if (StringUtils.isBlank(userInput)) {
            throw new IllegalArgumentException("anthropic messages request must contain latest user text content");
        }
        return new MessagesRequestContext(model, systemPrompt, userInput);
    }

    private String extractSystemPrompt(Object systemValue) {
        if (systemValue instanceof String text) {
            return StringUtils.trimToNull(text);
        }
        if (systemValue instanceof JSONArray blocks) {
            return collectTextBlocks(blocks);
        }
        return null;
    }

    private String extractLatestUserText(JSONArray messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            JSONObject message = messages.getJSONObject(i);
            if (message == null || !"user".equals(message.getString("role"))) {
                continue;
            }
            Object content = message.get("content");
            if (content instanceof String text) {
                return StringUtils.trimToNull(text);
            }
            if (content instanceof JSONArray blocks) {
                return collectTextBlocks(blocks);
            }
        }
        return null;
    }

    private String collectTextBlocks(JSONArray blocks) {
        List<String> parts = new ArrayList<>();
        for (int i = 0; i < blocks.size(); i++) {
            JSONObject block = blocks.getJSONObject(i);
            if (block == null) {
                continue;
            }
            if ("text".equals(block.getString("type")) && StringUtils.isNotBlank(block.getString("text"))) {
                parts.add(block.getString("text"));
                continue;
            }
            if ("tool_result".equals(block.getString("type")) && StringUtils.isNotBlank(block.getString("content"))) {
                parts.add(block.getString("content"));
            }
        }
        if (parts.isEmpty()) {
            return null;
        }
        return String.join("\n", parts);
    }

    private boolean hasProviderSessionId(StreamJsonEvent event) {
        return event != null && StringUtils.isNotBlank(event.session_id());
    }

    private boolean isMissingSessionError(StreamJsonEvent event) {
        if (event == null || !"result".equals(event.type()) || !Boolean.TRUE.equals(event.isError())) {
            return false;
        }

        List<String> messages = new ArrayList<>();
        if (StringUtils.isNotBlank(event.error())) {
            messages.add(event.error());
        }
        if (StringUtils.isNotBlank(event.result())) {
            messages.add(event.result());
        }
        if (event.errors() != null) {
            messages.addAll(event.errors().stream()
                    .filter(StringUtils::isNotBlank)
                    .toList());
        }

        return messages.stream().anyMatch(message -> message.contains("No conversation found with session ID:"));
    }

    private Flux<MessagesEvent> errorEvents(String message) {
        return Flux.just(new MessagesEvent("error", JSON.toJSONString(Map.of(
                "type", "error",
                "error", Map.of(
                        "type", "api_error",
                        "message", message
                )
        ))));
    }

    private void releaseAndEvictStaleSession(String sessionCode) {
        sessionRegistry.release(sessionCode);
        try {
            sessionRegistry.evict(sessionCode);
        } catch (SessionBusyException e) {
            log.warn("Stale session still busy when evicting, sessionCode={}", sessionCode);
        }
    }

    private void syncProviderSession(String sessionCode, String providerSessionId) {
        String currentProviderSessionId = StringUtils.trimToNull(sessionApi.getProviderSessionId(
                SessionRelationTargetType.AGENT_SESSION, sessionCode, AgentProvider.CLAUDE_CODE));
        if (currentProviderSessionId == null) {
            sessionApi.bind(
                    SessionRelationTargetType.AGENT_SESSION,
                    sessionCode,
                    AgentProvider.CLAUDE_CODE,
                    providerSessionId
            );
            return;
        }
        if (!StringUtils.equals(currentProviderSessionId, providerSessionId)) {
            sessionApi.rebind(
                    SessionRelationTargetType.AGENT_SESSION,
                    sessionCode,
                    AgentProvider.CLAUDE_CODE,
                    providerSessionId
            );
        }
    }
}
