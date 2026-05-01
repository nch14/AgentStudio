package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnthropicMessageAssemblerTest {

    @Test
    void shouldAssembleFinalMessageFromStreamingEvents() {
        AnthropicMessageAssembler assembler = new AnthropicMessageAssembler();

        assembler.onEvent(new MessagesEvent("message_start", """
                {
                  "type": "message_start",
                  "message": {
                    "id": "msg_1",
                    "type": "message",
                    "role": "assistant",
                    "content": [],
                    "model": "claude-sonnet",
                    "stop_reason": null,
                    "stop_sequence": null
                  }
                }
                """));
        assembler.onEvent(new MessagesEvent("content_block_start", """
                {
                  "type": "content_block_start",
                  "index": 0,
                  "content_block": {
                    "type": "text",
                    "text": ""
                  }
                }
                """));
        assembler.onEvent(new MessagesEvent("content_block_delta", """
                {
                  "type": "content_block_delta",
                  "index": 0,
                  "delta": {
                    "type": "text_delta",
                    "text": "hello"
                  }
                }
                """));
        assembler.onEvent(new MessagesEvent("message_delta", """
                {
                  "type": "message_delta",
                  "delta": {
                    "stop_reason": "end_turn",
                    "stop_sequence": null
                  },
                  "usage": {
                    "output_tokens": 5
                  }
                }
                """));

        JSONObject finalMessage = JSONObject.parseObject(assembler.toFinalMessageJson());
        assertEquals("msg_1", finalMessage.getString("id"));
        assertEquals("hello", finalMessage.getJSONArray("content").getJSONObject(0).getString("text"));
        assertEquals("end_turn", finalMessage.getString("stop_reason"));
        assertEquals(5, finalMessage.getJSONObject("usage").getIntValue("output_tokens"));
    }
}
