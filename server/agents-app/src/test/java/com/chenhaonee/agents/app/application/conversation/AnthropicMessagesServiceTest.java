package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
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
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.model.MessageStatus;
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
    private MessagesAgent messagesAgent;

    @InjectMocks
    private AnthropicMessagesService anthropicMessagesService;

    @Test
    void shouldReturnFinalMessageJsonForNonStreamingRequest() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        AgentSession session = session("session-1", "agent-a");
        AgentMessage assistantPlaceholder = assistantMessage("assistant-1", "session-1");
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "stream": false,
                  "messages": [
                    {
                      "role": "user",
                      "content": [
                        {
                          "type": "text",
                          "text": "hello"
                        }
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
                          "content_block": {
                            "type": "text",
                            "text": ""
                          }
                        }
                        """),
                new MessagesEvent("content_block_delta", """
                        {
                          "type": "content_block_delta",
                          "index": 0,
                          "delta": {
                            "type": "text_delta",
                            "text": "hello"
                          }
                        }
                        """),
                new MessagesEvent("message_delta", """
                        {
                          "type": "message_delta",
                          "delta": {
                            "stop_reason": "end_turn",
                            "stop_sequence": null
                          }
                        }
                        """)
        );

        when(agentDomainService.requireEnabledAgent("agent-a")).thenReturn(agent);
        when(providerCapabilityService.supportsMessages(AgentProvider.CLAUDE_CODE)).thenReturn(true);
        when(agentSessionRepository.findByCodeAndAgentCode("session-1", "agent-a")).thenReturn(Optional.of(session));
        when(agentRegistry.findMessagesAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(messagesAgent));
        when(messagesAgent.stream("agent-a", requestJson, "session-1")).thenReturn(result);
        when(agentSessionDomainService.appendMessage(eq("session-1"), any(AgentMessage.class))).thenReturn(session);
        when(agentSessionDomainService.appendMessageAndReturnMessage(eq("session-1"), any(AgentMessage.class)))
                .thenReturn(assistantPlaceholder);
        when(agentSessionDomainService.completeMessage(eq("assistant-1"), any(String.class), eq("msg_1")))
                .thenReturn(assistantPlaceholder);

        AnthropicMessagesService.AnthropicMessagesResult response = anthropicMessagesService.create("agent-a", "session-1", requestJson);

        assertEquals("session-1", response.sessionCode());
        assertNull(response.events());
        assertNotNull(response.messageJson());

        JSONObject finalMessage = JSON.parseObject(response.messageJson());
        assertEquals("msg_1", finalMessage.getString("id"));
        assertEquals("assistant", finalMessage.getString("role"));
        assertEquals("end_turn", finalMessage.getString("stop_reason"));
        assertEquals("hello", finalMessage.getJSONArray("content").getJSONObject(0).getString("text"));

        ArgumentCaptor<AgentMessage> userMessageCaptor = ArgumentCaptor.forClass(AgentMessage.class);
        verify(agentSessionDomainService).appendMessage(eq("session-1"), userMessageCaptor.capture());
        assertEquals(MessageRole.USER, userMessageCaptor.getValue().getRole());

        ArgumentCaptor<String> assistantPayloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(agentSessionDomainService).completeMessage(eq("assistant-1"), assistantPayloadCaptor.capture(), eq("msg_1"));
        JSONObject persistedPayload = JSON.parseObject(assistantPayloadCaptor.getValue());
        assertEquals("hello", persistedPayload.getJSONArray("content").getJSONObject(0).getString("text"));
    }

    @Test
    void shouldReturnServerSentEventsForStreamingRequest() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        AgentSession session = session("session-1", "agent-a");
        AgentMessage assistantPlaceholder = assistantMessage("assistant-1", "session-1");
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "stream": true,
                  "messages": [
                    {
                      "role": "user",
                      "content": "hello"
                    }
                  ]
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
        when(agentSessionDomainService.appendMessage(eq("session-1"), any(AgentMessage.class))).thenReturn(session);
        when(agentSessionDomainService.appendMessageAndReturnMessage(eq("session-1"), any(AgentMessage.class)))
                .thenReturn(assistantPlaceholder);
        when(agentSessionDomainService.completeMessage(eq("assistant-1"), any(String.class), eq("msg_1")))
                .thenReturn(assistantPlaceholder);

        AnthropicMessagesService.AnthropicMessagesResult response = anthropicMessagesService.create("agent-a", "session-1", requestJson);
        List<ServerSentEvent<String>> events = response.events().collectList().block();

        assertEquals("session-1", response.sessionCode());
        assertNull(response.messageJson());
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
                  "messages": [
                    {
                      "role": "user",
                      "content": "hello"
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

        assertEquals("anthropic messages request must contain positive max_tokens", error.getMessage());
    }

    @Test
    void shouldRejectWhenCapabilityServiceIndicatesNoMessagesSupport() {
        Agent agent = enabledAgent("agent-a", AgentProvider.CLAUDE_CODE);
        String requestJson = """
                {
                  "model": "claude-sonnet",
                  "max_tokens": 1024,
                  "messages": [
                    {
                      "role": "user",
                      "content": "hello"
                    }
                  ]
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
                        {
                          "type": "image",
                          "source": {
                            "type": "base64",
                            "media_type": "image/png",
                            "data": "abc"
                          }
                        }
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
                "anthropic messages currently only support text and tool_result user content blocks",
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

    private AgentMessage assistantMessage(String messageCode, String sessionCode) {
        AgentMessage message = new AgentMessage();
        message.setCode(messageCode);
        message.setSessionCode(sessionCode);
        message.setRole(MessageRole.ASSISTANT);
        message.setStatus(MessageStatus.STREAMING);
        return message;
    }
}
