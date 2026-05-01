package com.chenhaonee.agents.claudecode.stream;

import com.alibaba.fastjson2.JSON;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;

/**
 * 将 Claude Code 单轮 stream-json 事件投影为 Messages SPI 事件。
 */
public class StreamJsonToMessagesEventMapper {

    public Flux<MessagesEvent> project(Flux<StreamJsonEvent> source) {
        return Flux.defer(() -> {
            ProjectionState state = new ProjectionState();
            return source.concatMapIterable(event -> projectEvent(event, state));
        });
    }

    private List<MessagesEvent> projectEvent(StreamJsonEvent event, ProjectionState state) {
        if (event == null) {
            return Collections.emptyList();
        }

        if ("stream_event".equals(event.type()) && event.event() != null && StringUtils.isNotBlank(event.streamEventType())) {
            if ("message_delta".equals(event.streamEventType())) {
                state.sawMessageDelta = true;
            }
            if ("message_stop".equals(event.streamEventType())) {
                state.sawMessageStop = true;
            }
            return List.of(new MessagesEvent(event.streamEventType(), JSON.toJSONString(event.event())));
        }

        if (!"result".equals(event.type())) {
            return Collections.emptyList();
        }

        if (Boolean.TRUE.equals(event.isError())) {
            return List.of(new MessagesEvent("error", JSON.toJSONString(Map.of(
                    "type", "error",
                    "error", buildErrorBody(event)
            ))));
        }

        if (state.sawMessageStop) {
            return Collections.emptyList();
        }
        List<MessagesEvent> projected = new ArrayList<>(2);
        if (!state.sawMessageDelta) {
            projected.add(new MessagesEvent("message_delta", JSON.toJSONString(buildSyntheticMessageDelta())));
            state.sawMessageDelta = true;
        }
        projected.add(new MessagesEvent("message_stop", JSON.toJSONString(Map.of("type", "message_stop"))));
        state.sawMessageStop = true;
        return projected;
    }

    private Map<String, Object> buildErrorBody(StreamJsonEvent event) {
        String message = StringUtils.firstNonBlank(
                event.error(),
                event.result(),
                joinErrors(event),
                "Unknown Claude Code error"
        );
        return Map.of(
                "type", "api_error",
                "message", message
        );
    }

    private Map<String, Object> buildSyntheticMessageDelta() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "message_delta");

        Map<String, Object> delta = new LinkedHashMap<>();
        delta.put("stop_reason", "end_turn");
        delta.put("stop_sequence", null);
        body.put("delta", delta);

        return body;
    }

    private String joinErrors(StreamJsonEvent event) {
        if (event.errors() == null || event.errors().isEmpty()) {
            return null;
        }
        return event.errors().stream()
                .filter(StringUtils::isNotBlank)
                .reduce((left, right) -> left + "\n" + right)
                .orElse(null);
    }

    private static final class ProjectionState {
        private boolean sawMessageDelta;
        private boolean sawMessageStop;
    }
}
