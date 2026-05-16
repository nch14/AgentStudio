package com.chenhaonee.agents.app.conversation;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.chenhaonee.agents.app.interfaces.http.conversation.AgentConversationSessionController;
import com.chenhaonee.agents.domain.session.model.AgentMessage;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.model.MessageStatus;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class AgentConversationSessionControllerTest {

    private final AgentSessionDomainService agentSessionDomainService =
            org.mockito.Mockito.mock(AgentSessionDomainService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new AgentConversationSessionController(agentSessionDomainService)).build();

    private AgentSession sampleSession;
    private AgentMessage sampleMessage;

    @BeforeEach
    void setUp() {
        sampleSession = new AgentSession();
        sampleSession.setCode("session-001");
        sampleSession.setAgentCode("agent-001");
        sampleSession.setTitle("今日摘要");
        sampleSession.setMessageCount(3);
        sampleSession.setArchived(false);
        sampleSession.setLastMessageTime(Instant.parse("2026-04-21T15:00:00Z"));

        sampleMessage = new AgentMessage();
        sampleMessage.setCode("msg-001");
        sampleMessage.setSessionCode("session-001");
        sampleMessage.setTurnCode("turn-001");
        sampleMessage.setRole(MessageRole.ASSISTANT);
        sampleMessage.setProtocolType(MessageProtocolType.ANTHROPIC_MESSAGES);
        sampleMessage.setStatus(MessageStatus.COMPLETED);
        sampleMessage.setPayloadJson("{\"id\":\"msg_1\",\"type\":\"message\"}");
        sampleMessage.setExternalMessageId("msg_1");
        sampleMessage.assignMessageIndex(2);
    }

    @Test
    void shouldListAgentSessions() throws Exception {
        Page<AgentSession> page = new PageImpl<>(List.of(sampleSession), PageRequest.of(0, 20), 1);
        when(agentSessionDomainService.listSessionsByAgent(0, 20, "agent-001", false, SessionScene.CHAT)).thenReturn(page);

        mockMvc.perform(get("/api/v1/agents/{agentCode}/sessions", "agent-001")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.data[0].code").value("session-001"))
                .andExpect(jsonPath("$.data[0].title").value("今日摘要"));
    }

    @Test
    void shouldReturnSessionMessagesWithProtocolMetadata() throws Exception {
        Page<AgentMessage> page = new PageImpl<>(List.of(sampleMessage), PageRequest.of(0, 50), 1);
        when(agentSessionDomainService.listMessages("agent-001", "session-001", PageRequest.of(0, 50))).thenReturn(page);

        mockMvc.perform(get("/api/v1/agents/{agentCode}/sessions/{sessionCode}/messages", "agent-001", "session-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].code").value("msg-001"))
                .andExpect(jsonPath("$.data[0].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.data[0].protocolType").value("ANTHROPIC_MESSAGES"))
                .andExpect(jsonPath("$.data[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$.data[0].externalMessageId").value("msg_1"));
    }

    @Test
    void shouldArchiveSession() throws Exception {
        doNothing().when(agentSessionDomainService).archive("agent-001", "session-001");

        mockMvc.perform(post("/api/v1/agents/{agentCode}/sessions/{sessionCode}/archive", "agent-001", "session-001")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(java.util.Map.of())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.errorMessage").value("会话已归档"));
    }
}
