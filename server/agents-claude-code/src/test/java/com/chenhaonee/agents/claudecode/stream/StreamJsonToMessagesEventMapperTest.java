package com.chenhaonee.agents.claudecode.stream;

import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import java.util.List;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StreamJsonToMessagesEventMapperTest {

    private final StreamJsonParser parser = new StreamJsonParser();
    private final StreamJsonToMessagesEventMapper mapper = new StreamJsonToMessagesEventMapper();

    @Test
    void shouldPassThroughStreamEventAndSupplementSuccessfulResultTail() {
        StreamJsonEvent messageStart = parse("""
                {"type":"stream_event","event":{"type":"message_start","message":{"id":"msg_1","type":"message","role":"assistant","content":[]}},"session_id":"provider-session-1"}
                """);
        StreamJsonEvent delta = parse("""
                {"type":"stream_event","event":{"type":"content_block_delta","index":1,"delta":{"type":"text_delta","text":"你好"}},"session_id":"provider-session-1"}
                """);
        StreamJsonEvent result = parse("""
                {"type":"result","subtype":"success","session_id":"provider-session-1","is_error":false}
                """);

        List<MessagesEvent> projected = mapper.project(Flux.just(messageStart, delta, result))
                .collectList()
                .block();

        assertEquals(4, projected.size());
        assertEquals("message_start", projected.get(0).eventType());
        assertEquals("content_block_delta", projected.get(1).eventType());
        assertEquals("message_delta", projected.get(2).eventType());
        assertEquals("message_stop", projected.get(3).eventType());

        JSONObject messageDelta = JSONObject.parseObject(projected.get(2).dataJson());
        assertEquals("message_delta", messageDelta.getString("type"));
        assertEquals("end_turn", messageDelta.getJSONObject("delta").getString("stop_reason"));
    }

    @Test
    void shouldNotDuplicateStopEventWhenClaudeAlreadyProvidedIt() {
        StreamJsonEvent messageStop = parse("""
                {"type":"stream_event","event":{"type":"message_stop"},"session_id":"provider-session-1"}
                """);
        StreamJsonEvent result = parse("""
                {"type":"result","subtype":"success","session_id":"provider-session-1","is_error":false}
                """);

        List<MessagesEvent> projected = mapper.project(Flux.just(messageStop, result))
                .collectList()
                .block();

        assertEquals(1, projected.size());
        assertEquals("message_stop", projected.getFirst().eventType());
    }

    @Test
    void shouldMapErroredResultToAnthropicErrorEvent() {
        StreamJsonEvent event = parse("""
                {"type":"result","subtype":"error","session_id":"provider-session-1","error":"rate limited","is_error":true}
                """);

        List<MessagesEvent> projected = mapper.project(Flux.just(event)).collectList().block();

        assertEquals(1, projected.size());
        assertEquals("error", projected.getFirst().eventType());
        JSONObject data = JSONObject.parseObject(projected.getFirst().dataJson());
        assertEquals("error", data.getString("type"));
        assertEquals("api_error", data.getJSONObject("error").getString("type"));
        assertEquals("rate limited", data.getJSONObject("error").getString("message"));
    }

    private StreamJsonEvent parse(String line) {
        return parser.parseLine(line);
    }
}
