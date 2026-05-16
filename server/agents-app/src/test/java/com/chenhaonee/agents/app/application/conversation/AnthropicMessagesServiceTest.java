package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.agent.ProviderCapabilityService;
import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.connect.support.AttachmentResolver;
import com.chenhaonee.agents.connect.support.MessagesEventBlockRecorderFactory;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import com.chenhaonee.agents.domain.session.factory.AgentSessionDomainFactory;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.ContentBlockType;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.repository.AgentSessionRepository;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnthropicMessagesServiceTest {

    @Mock
    private AgentSessionRepository agentSessionRepository;

    @Mock
    private AgentSessionDomainService agentSessionDomainService;

    @Mock
    private AgentSessionDomainFactory agentSessionDomainFactory;

    @Mock
    private AgentDomainService agentDomainService;

    @Mock
    private AgentRegistry agentRegistry;

    @Mock
    private ProviderCapabilityService providerCapabilityService;

    @Mock
    private MessagesEventBlockRecorderFactory messagesEventBlockRecorderFactory;

    @Mock
    private MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder recorder;

    @Mock
    private ActiveStreamRegistry activeStreamRegistry;

    @Mock
    private AttachmentValidator attachmentValidator;

    @Mock
    private AnthropicContentBlockBuilder anthropicContentBlockBuilder;

    @Mock
    private AttachmentResolver attachmentResolver;

    @Mock
    private ClaudeCodeProperties claudeCodeProperties;

    @Mock
    private MessagesAgent messagesAgent;

    @InjectMocks
    private AnthropicMessagesService anthropicMessagesService;

    @Test
    void shouldReturnFinalMessageJsonForNonStreamingRequest() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        AgentSession session = session("session-1", "agent-a");
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "stream": false,
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {"type": "text", "text": "hello"}
                      ]
                    }
                  ]
                }
                """;
        Flux<MessagesEvent> result = Flux.just(
                new MessagesEvent("message_start", """
                        {
                          "type": "message_start",
                          "message": {
                            "id": "msg_1",
                            "type": "message",
                            "role": "assistant",
                            "model": "claude-sonnet",
                            "content": [],
                            "stop_reason": null,
                            "stop_sequence": null
                          }
                        }
                        """),
                new MessagesEvent("content_block_start", """
                        {
                          "type": "content_block_start",
                          "index": 0,
                          "content_block": {"type": "text", "text": ""}
                        }
                        """),
                new MessagesEvent("content_block_delta", """
                        {
                          "type": "content_block_delta",
                          "index": 0,
                          "delta": {"type": "text_delta", "text": "hello"}
                        }
                        """),
                new MessagesEvent("content_block_stop", """
                        {"type": "content_block_stop", "index": 0}
                        """),
                new MessagesEvent("message_delta", """
                        {
                          "type": "message_delta",
                          "delta": {"stop_reason": "end_turn", "stop_sequence": null}
                        }
                        """),
                new MessagesEvent("message_stop", "{\"type\":\"message_stop\"}")
        );

        when(agentDomainService.requireEnabledAgent("agent-a")).thenReturn(agent);
        when(providerCapabilityService.supportsMessages(AgentProvider.CLAUDE_CODE)).thenReturn(true);
        when(agentSessionRepository.findByCodeAndAgentCode("session-1", "agent-a")).thenReturn(Optional.of(session));
        when(agentRegistry.findMessagesAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(messagesAgent));
        when(messagesAgent.stream("agent-a", requestJson, "session-1")).thenReturn(result);
        when(messagesEventBlockRecorderFactory.create(eq("session-1"), any(), eq(MessageProtocolType.ANTHROPIC_MESSAGES)))
                .thenReturn(recorder);

        AnthropicMessagesService.AnthropicMessagesResult response =
                anthropicMessagesService.create("agent-a", "session-1", requestJson);

        assertEquals("session-1", response.sessionCode());
        assertNull(response.events());
        assertNotNull(response.messageJson());

        JSONObject finalMessage = JSON.parseObject(response.messageJson());
        assertEquals("msg_1", finalMessage.getString("id"));
        assertEquals("assistant", finalMessage.getString("role"));
        assertEquals("end_turn", finalMessage.getString("stop_reason"));
        assertEquals("hello", finalMessage.getJSONArray("content").getJSONObject(0).getString("text"));

        ArgumentCaptor<String> userPayload = ArgumentCaptor.forClass(String.class);
        verify(agentSessionDomainService).appendBlock(
                eq("session-1"), any(), eq(MessageRole.USER), eq(ContentBlockType.TEXT),
                eq(MessageProtocolType.ANTHROPIC_MESSAGES), userPayload.capture(), eq((String) null)
        );
        assertEquals("hello", JSON.parseObject(userPayload.getValue()).getString("text"));
    }

    @Test
    void shouldReturnServerSentEventsForStreamingRequest() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        AgentSession session = session("session-1", "agent-a");
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "stream": true,
                  "messages": [{"role": "user", "content": "hello"}]
                }
                """;
        Flux<MessagesEvent> result = Flux.just(
                new MessagesEvent("message_start", "{\"message\":{\"id\":\"msg_1\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[]}}"),
                new MessagesEvent("message_stop", "{\"type\":\"message_stop\"}")
        );

        when(agentDomainService.requireEnabledAgent("agent-a")).thenReturn(agent);
        when(providerCapabilityService.supportsMessages(AgentProvider.CLAUDE_CODE)).thenReturn(true);
        when(agentSessionRepository.findByCodeAndAgentCode("session-1", "agent-a")).thenReturn(Optional.of(session));
        when(agentRegistry.findMessagesAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(messagesAgent));
        when(messagesAgent.stream("agent-a", requestJson, "session-1")).thenReturn(result);
        when(messagesEventBlockRecorderFactory.create(eq("session-1"), any(), eq(MessageProtocolType.ANTHROPIC_MESSAGES)))
                .thenReturn(recorder);

        AnthropicMessagesService.AnthropicMessagesResult response =
                anthropicMessagesService.create("agent-a", "session-1", requestJson);

        assertEquals("session-1", response.sessionCode());
        assertNull(response.messageJson());

        // 取出 SSE 流，仅过滤标准事件（含 message_start / message_stop），剔除 keepalive 注释行
        List<ServerSentEvent<String>> events = response.events()
                .filter(sse -> sse.event() != null)
                .collectList()
                .block();
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("message_start", events.getFirst().event());
        assertEquals("message_stop", events.get(1).event());
        assertTrue(events.getFirst().data().contains("\"msg_1\""));
    }

    @Test
    void shouldRequirePositiveMaxTokens() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "messages": [{"role": "user", "content": "hello"}]
                }
                """;

        when(agentDomainService.requireEnabledAgent("agent-a")).thenReturn(agent);
        when(providerCapabilityService.supportsMessages(AgentProvider.CLAUDE_CODE)).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> anthropicMessagesService.create("agent-a", "session-1", requestJson)
        );

        assertEquals("anthropic messages request must contain positive max_tokens", error.getMessage());
    }

    @Test
    void shouldRejectWhenCapabilityServiceIndicatesNoMessagesSupport() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "messages": [{"role": "user", "content": "hello"}]
                }
                """;

        when(agentDomainService.requireEnabledAgent("agent-a")).thenReturn(agent);
        when(providerCapabilityService.supportsMessages(AgentProvider.CLAUDE_CODE)).thenReturn(false);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> anthropicMessagesService.create("agent-a", "session-1", requestJson)
        );

        assertEquals("provider CLAUDE_CODE does not support messages", error.getMessage());
    }

    @Test
    void shouldRejectUnsupportedAnthropicUserBlocks() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {"type": "video", "source": {"type": "base64", "media_type": "video/mp4", "data": "abc"}}
                      ]
                    }
                  ]
                }
                """;

        when(agentDomainService.requireEnabledAgent("agent-a")).thenReturn(agent);
        when(providerCapabilityService.supportsMessages(AgentProvider.CLAUDE_CODE)).thenReturn(true);

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> anthropicMessagesService.create("agent-a", "session-1", requestJson)
        );

        assertEquals(
                "anthropic messages currently only support text / image / document / tool_result user content blocks",
                error.getMessage()
        );
    }

    private Agent enabledAgent(String agentCode, AgentProvider provider) {
        Agent agent = new Agent();
        agent.setCode(agentCode);
        agent.setProvider(provider);
        agent.enable();
        return agent;
    }

    private AgentSession session(String sessionCode, String agentCode) {
        AgentSession session = new AgentSession();
        session.setCode(sessionCode);
        session.setAgentCode(agentCode);
        session.setTitle("test");
        return session;
    }
}
