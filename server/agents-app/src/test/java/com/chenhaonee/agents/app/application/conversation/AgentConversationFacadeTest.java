package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService.AnthropicMessagesResult;
import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgentConversationFacadeTest {

    @Mock
    private AgentDomainService agentDomainService;

    @Mock
    private AnthropicMessagesService anthropicMessagesService;

    private final ClaudeCodeProperties claudeCodeProperties = new ClaudeCodeProperties();

    private ConversationRuntimePolicyResolver conversationRuntimePolicyResolver =
            new ConversationRuntimePolicyResolver(claudeCodeProperties);

    private AgentConversationFacade agentConversationFacade;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        agentConversationFacade = new AgentConversationFacade(
                agentDomainService,
                conversationRuntimePolicyResolver,
                anthropicMessagesService
        );
    }

    @Test
    void shouldRouteClaudeCodeToAnthropicMessagesAndBuildServerPolicyRequest() {
        Agent agent = enabledAgent("agent-claude", AgentProvider.CLAUDE_CODE, Map.of("model", "claude-opus-4"));
        Flux<ServerSentEvent<String>> events = Flux.just(ServerSentEvent.builder("{\"type\":\"message_start\"}").build());

        when(agentDomainService.requireEnabledAgent("agent-claude")).thenReturn(agent);
        when(anthropicMessagesService.create(eq("agent-claude"), eq(null), org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(AnthropicMessagesResult.streaming("session-new", events));

        AgentConversationFacade.StreamingConversationResult result =
                agentConversationFacade.sendMessage("agent-claude", null, "请总结今天的新闻");

        ArgumentCaptor<String> requestCaptor = ArgumentCaptor.forClass(String.class);
        verify(anthropicMessagesService).create(eq("agent-claude"), eq(null), requestCaptor.capture());
        JSONObject requestJson = JSON.parseObject(requestCaptor.getValue());

        assertEquals("session-new", result.sessionCode());
        assertEquals(ConversationRuntimePolicy.ANTHROPIC_MESSAGES, result.protocolType());
        assertFalse(requestJson.getBooleanValue("stream") == false);
        assertEquals("claude-opus-4", requestJson.getString("model"));
        assertEquals(8192, requestJson.getIntValue("max_tokens"));

        JSONArray messages = requestJson.getJSONArray("messages");
        assertEquals(1, messages.size());
        assertEquals("user", messages.getJSONObject(0).getString("role"));
        assertEquals("请总结今天的新闻", messages.getJSONObject(0).getString("content"));
    }

    private Agent enabledAgent(String code, AgentProvider provider, Map<String, String> providerConfig) {
        Agent agent = new Agent();
        agent.setCode(code);
        agent.setProvider(provider);
        agent.setProviderConfig(providerConfig);
        agent.setStatus(Status.ENABLED);
        return agent;
    }
}
