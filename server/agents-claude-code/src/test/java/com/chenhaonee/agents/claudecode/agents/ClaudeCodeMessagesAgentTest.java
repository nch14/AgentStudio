package com.chenhaonee.agents.claudecode.agents;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.process.ChatProcessSpec;
import com.chenhaonee.agents.claudecode.process.ChatSessionRegistry;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcess;
import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;
import com.chenhaonee.agents.claudecode.stream.StreamJsonParser;
import com.chenhaonee.agents.claudecode.workspace.WorkspaceManager;
import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.capability.SessionApi;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaudeCodeMessagesAgentTest {

    private static final String DEFAULT_MODEL = "claude-default";

    private final StreamJsonParser parser = new StreamJsonParser();

    @Mock
    private AgentConfigApi agentConfigApi;

    @Mock
    private WorkspaceManager workspaceManager;

    @Mock
    private ChatSessionRegistry sessionRegistry;

    @Mock
    private SessionApi sessionApi;

    @Mock
    private ClaudeCodeProcess process;

    @Test
    void shouldBindResolvedProviderSessionAndPassThroughStreamEvents() throws Exception {
        ClaudeCodeMessagesAgent agent = newMessagesAgent();
        Path agentHome = Path.of("/tmp/agent-1");
        StreamJsonEvent messageStart = parse("""
                {"type":"stream_event","event":{"type":"message_start","message":{"id":"msg_1","type":"message","role":"assistant","content":[],"model":"claude-sonnet","stop_reason":null,"stop_sequence":null}},"session_id":"provider-session-1"}
                """);
        StreamJsonEvent deltaEvent = parse("""
                {"type":"stream_event","event":{"type":"content_block_delta","index":0,"delta":{"type":"text_delta","text":"hello"}},"session_id":"provider-session-1"}
                """);
        StreamJsonEvent resultEvent = parse("""
                {"type":"result","subtype":"success","session_id":"provider-session-1","is_error":false}
                """);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of("model", DEFAULT_MODEL));
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.AGENT_SESSION, "session-1", AgentProvider.CLAUDE_CODE
        )).thenReturn(null);
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(agentHome);
        when(sessionRegistry.acquire(
                eq("session-1"),
                argThat(spec -> matchesSpec(spec, agentHome, "claude-sonnet", "system prompt")),
                eq(null)
        )).thenReturn(process);
        when(process.startTurn(any(JSONArray.class))).thenReturn(Flux.just(messageStart, deltaEvent, resultEvent));

        List<MessagesEvent> events = agent.stream("agent-1", """
                {
                  "model": "claude-sonnet",
                  "system": "system prompt",
                  "messages": [
                    {"role": "user", "content": "hello"}
                  ],
                  "stream": true
                }
                """, "session-1").collectList().block();

        assertEquals(6, events.size());
        assertEquals("turn_start", events.get(0).eventType());
        assertEquals("message_start", events.get(1).eventType());
        assertEquals("content_block_delta", events.get(2).eventType());
        assertEquals("message_delta", events.get(3).eventType());
        assertEquals("message_stop", events.get(4).eventType());
        assertEquals("turn_stop", events.get(5).eventType());
        JSONObject delta = JSONObject.parseObject(events.get(2).dataJson());
        assertEquals("hello", delta.getJSONObject("delta").getString("text"));
        JSONObject messageDelta = JSONObject.parseObject(events.get(3).dataJson());
        assertEquals("end_turn", messageDelta.getJSONObject("delta").getString("stop_reason"));
        verify(sessionApi).bind(
                SessionRelationTargetType.AGENT_SESSION,
                "session-1",
                AgentProvider.CLAUDE_CODE,
                "provider-session-1"
        );
    }

    @Test
    void shouldMapErroredResultToErrorEvent() throws Exception {
        ClaudeCodeMessagesAgent agent = newMessagesAgent();
        Path agentHome = Path.of("/tmp/agent-1");
        StreamJsonEvent errorEvent = parse("""
                {"type":"result","subtype":"error","session_id":"provider-session-1","error":"rate limited","is_error":true}
                """);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of("model", DEFAULT_MODEL));
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.AGENT_SESSION, "session-1", AgentProvider.CLAUDE_CODE
        )).thenReturn(null);
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(agentHome);
        when(sessionRegistry.acquire(
                eq("session-1"),
                argThat(spec -> matchesSpec(spec, agentHome, DEFAULT_MODEL, null)),
                eq(null)
        )).thenReturn(process);
        when(process.startTurn(any(JSONArray.class))).thenReturn(Flux.just(errorEvent));

        List<MessagesEvent> events = agent.stream("agent-1", """
                {
                  "messages": [
                    {"role": "user", "content": "hello"}
                  ],
                  "stream": true
                }
                """, "session-1").collectList().block();

        assertEquals(2, events.size());
        assertEquals("turn_start", events.get(0).eventType());
        assertEquals("error", events.get(1).eventType());
        JSONObject body = JSONObject.parseObject(events.get(1).dataJson());
        assertEquals("api_error", body.getJSONObject("error").getString("type"));
        assertEquals("rate limited", body.getJSONObject("error").getString("message"));
    }

    @Test
    void shouldExtractSystemArrayAndLatestUserTextBlocks() throws Exception {
        ClaudeCodeMessagesAgent agent = newMessagesAgent();
        Path agentHome = Path.of("/tmp/agent-1");
        StreamJsonEvent resultEvent = parse("""
                {"type":"result","subtype":"success","session_id":"provider-session-1","is_error":false}
                """);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of("model", DEFAULT_MODEL));
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.AGENT_SESSION, "session-1", AgentProvider.CLAUDE_CODE
        )).thenReturn(null);
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(agentHome);
        when(sessionRegistry.acquire(
                eq("session-1"),
                argThat(spec -> matchesSpec(spec, agentHome, DEFAULT_MODEL, "sys-a\nsys-b")),
                eq(null)
        )).thenReturn(process);
        when(process.startTurn(any(JSONArray.class))).thenReturn(Flux.just(resultEvent));

        List<MessagesEvent> events = agent.stream("agent-1", """
                {
                  "system": [
                    {"type": "text", "text": "sys-a"},
                    {"type": "text", "text": "sys-b"}
                  ],
                  "messages": [
                    {"role": "assistant", "content": "ignored"},
                    {"role": "user", "content": [
                      {"type": "text", "text": "part-1"},
                      {"type": "text", "text": "part-2"}
                    ]}
                  ],
                  "stream": true
                }
                """, "session-1").collectList().block();

        assertEquals(4, events.size());
        assertEquals("turn_start", events.get(0).eventType());
        assertEquals("message_delta", events.get(1).eventType());
        assertEquals("message_stop", events.get(2).eventType());
        assertEquals("turn_stop", events.get(3).eventType());
    }

    @Test
    void shouldNotDuplicateMessageStopWhenClaudeAlreadyProvidedIt() throws Exception {
        ClaudeCodeMessagesAgent agent = newMessagesAgent();
        Path agentHome = Path.of("/tmp/agent-1");
        StreamJsonEvent messageStop = parse("""
                {"type":"stream_event","event":{"type":"message_stop"},"session_id":"provider-session-1"}
                """);
        StreamJsonEvent resultEvent = parse("""
                {"type":"result","subtype":"success","session_id":"provider-session-1","is_error":false}
                """);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of("model", DEFAULT_MODEL));
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.AGENT_SESSION, "session-1", AgentProvider.CLAUDE_CODE
        )).thenReturn(null);
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(agentHome);
        when(sessionRegistry.acquire(
                eq("session-1"),
                argThat(spec -> matchesSpec(spec, agentHome, DEFAULT_MODEL, null)),
                eq(null)
        )).thenReturn(process);
        when(process.startTurn(any(JSONArray.class))).thenReturn(Flux.just(messageStop, resultEvent));

        List<MessagesEvent> events = agent.stream("agent-1", """
                {
                  "messages": [
                    {"role": "user", "content": "hello"}
                  ],
                  "stream": true
                }
                """, "session-1").collectList().block();

        assertEquals(3, events.size());
        assertEquals("turn_start", events.get(0).eventType());
        assertEquals("message_stop", events.get(1).eventType());
        assertEquals("turn_stop", events.get(2).eventType());
    }

    private ClaudeCodeMessagesAgent newMessagesAgent() {
        ClaudeCodeProperties properties = new ClaudeCodeProperties();
        properties.setDefaultModel(DEFAULT_MODEL);
        return new ClaudeCodeMessagesAgent(
                properties,
                agentConfigApi,
                workspaceManager,
                sessionRegistry,
                sessionApi
        );
    }

    private boolean matchesSpec(ChatProcessSpec spec, Path agentHome, String model, String systemPrompt) {
        return spec != null
                && agentHome.toString().equals(spec.workDir())
                && agentHome.resolve("mcp-config.json").toString().equals(spec.mcpConfigPath())
                && model.equals(spec.model())
                && ((systemPrompt == null && spec.systemPrompt() == null)
                || (systemPrompt != null && systemPrompt.equals(spec.systemPrompt())));
    }

    private StreamJsonEvent parse(String line) {
        return parser.parseLine(line);
    }
}
