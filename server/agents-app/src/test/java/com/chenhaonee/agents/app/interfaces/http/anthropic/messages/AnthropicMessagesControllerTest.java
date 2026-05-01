package com.chenhaonee.agents.app.interfaces.http.anthropic.messages;

import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AnthropicMessagesControllerTest {

    private final AnthropicMessagesService anthropicMessagesService = mock(AnthropicMessagesService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new AnthropicMessagesController(anthropicMessagesService)).build();

    @Test
    void shouldReturnMessageJsonAndSessionHeaders() throws Exception {
        when(anthropicMessagesService.create("agent-a", "session-1", "{\"model\":\"claude-sonnet\"}"))
                .thenReturn(AnthropicMessagesService.AnthropicMessagesResult.nonStreaming(
                        "session-1",
                        "{\"id\":\"msg_1\",\"type\":\"message\"}"));

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Agent-Code", "agent-a")
                        .header("X-Agent-Session-Code", "session-1")
                        .contentType("application/json")
                        .content("{\"model\":\"claude-sonnet\"}"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/json"))
                .andExpect(header().string("X-Agent-Code", "agent-a"))
                .andExpect(header().string("X-Agent-Session-Code", "session-1"))
                .andExpect(header().exists("request-id"))
                .andExpect(jsonPath("$.id").value("msg_1"))
                .andExpect(jsonPath("$.type").value("message"));
    }

    @Test
    void shouldReturnAnthropicStyleErrorBody() throws Exception {
        when(anthropicMessagesService.create("agent-a", null, "{\"model\":\"claude-sonnet\"}"))
                .thenThrow(new IllegalArgumentException("bad request"));

        mockMvc.perform(post("/api/v1/messages")
                        .header("X-Agent-Code", "agent-a")
                        .contentType("application/json")
                        .content("{\"model\":\"claude-sonnet\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType("application/json"))
                .andExpect(header().string("X-Agent-Code", "agent-a"))
                .andExpect(header().exists("request-id"))
                .andExpect(jsonPath("$.type").value("error"))
                .andExpect(jsonPath("$.error.type").value("invalid_request_error"))
                .andExpect(jsonPath("$.error.message").value("bad request"));
    }
}
