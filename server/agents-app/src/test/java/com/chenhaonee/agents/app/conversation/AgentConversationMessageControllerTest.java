package com.chenhaonee.agents.app.conversation;

import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService.AnthropicMessagesResult;
import com.chenhaonee.agents.app.interfaces.http.conversation.AgentConversationMessageController;
import org.junit.jupiter.api.Test;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import reactor.core.publisher.Flux;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AgentConversationMessageControllerTest {

    private final AnthropicMessagesService anthropicMessagesService = mock(AnthropicMessagesService.class);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
            new AgentConversationMessageController(anthropicMessagesService)).build();

    @Test
    void shouldAcceptContentOnlyAndAlwaysReturnSse() throws Exception {
        Flux<ServerSentEvent<String>> events = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("message_start")
                        .data("{\"type\":\"message_start\"}")
                        .build()
        );
        when(anthropicMessagesService.createStreaming("agent-a", "session-1", "你好", null))
                .thenReturn(AnthropicMessagesResult.streaming("session-1", events));

        MvcResult mvcResult = mockMvc.perform(post("/api/v1/agents/{agentCode}/messages", "agent-a")
                        .header("X-Agent-Session-Code", "session-1")
                        .contentType("application/json")
                        .content("{\"content\":\"你好\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(header().string("X-Agent-Code", "agent-a"))
                .andExpect(header().string("X-Agent-Session-Code", "session-1"))
                .andExpect(content().string(containsString("event:message_start")))
                .andExpect(content().string(containsString("data:{\"type\":\"message_start\"}")));
    }

    @Test
    void shouldReturnCreatedSessionHeaderWhenRequestDoesNotProvideSessionCode() throws Exception {
        Flux<ServerSentEvent<String>> events = Flux.just(
                ServerSentEvent.<String>builder()
                        .event("message")
                        .data("{\"id\":\"chatcmpl_1\"}")
                        .build()
        );
        when(anthropicMessagesService.createStreaming("agent-a", null, "帮我写个摘要", null))
                .thenReturn(AnthropicMessagesResult.streaming("session-new", events));

        MvcResult mvcResult = mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/agents/{agentCode}/messages", "agent-a")
                        .contentType("application/json")
                        .content("{\"content\":\"帮我写个摘要\"}"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/event-stream"))
                .andExpect(header().string("X-Agent-Code", "agent-a"))
                .andExpect(header().string("X-Agent-Session-Code", "session-new"));
    }
}
