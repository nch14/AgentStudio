package com.chenhaonee.agents.claudecode.stream;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.connect.support.AgentMessageBlockPayloads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 将 Claude Code 单轮 stream-json 事件投影为 Messages SPI 事件。
 *
 * <p>在 Anthropic 标准事件之上注入 turn_start / turn_stop 边界事件，把 chat 模式的
 * stream_event 直通；task 模式的 assistant 整块投影为 content_block_start/delta/stop
 * 三联事件以便 recorder 落库；user 中的 tool_result 块投影为 tool_result 伪事件。</p>
 */
public class StreamJsonToMessagesEventMapper {

    public Flux<MessagesEvent> project(Flux<StreamJsonEvent> source) {
        return Flux.defer(() -> {
            ProjectionState state = new ProjectionState();
            Flux<MessagesEvent> body = source.concatMapIterable(event -> projectEvent(event, state));
            return Mono.just(new MessagesEvent("turn_start", "{}")).concatWith(body);
        });
    }

    private List<MessagesEvent> projectEvent(StreamJsonEvent event, ProjectionState state) {
        if (event == null) {
            return Collections.emptyList();
        }

        // --- Chat-mode wrapped SSE events (type:stream_event) ---
        if ("stream_event".equals(event.type()) && event.event() != null && StringUtils.isNotBlank(event.streamEventType())) {
            String streamType = event.streamEventType();
            if ("message_start".equals(streamType)) {
                state.sawMessageStart = true;
            }
            if ("message_delta".equals(streamType)) {
                state.sawMessageDelta = true;
            }
            if ("message_stop".equals(streamType)) {
                state.sawMessageStop = true;
            }
            state.sawStreamEvent = true;
            return List.of(new MessagesEvent(streamType, JSON.toJSONString(event.event())));
        }

        // --- Task-mode raw assistant events (type:assistant) ---
        // Claude Code CLI --print --output-format stream-json --verbose emits:
        //   {"type":"assistant","message":{"role":"assistant","content":[{...},{...}]}}
        // 每个 content[] 元素就是一个 content block (text/thinking/tool_use)。
        // 翻译为 content_block_start/delta/stop 让 recorder 能落 TEXT / THINKING / TOOL_USE。
        //
        // Chat 模式 CLI 在所有 stream_event 增量后还会再发一个 assistant 全量快照，
        // 跳过它避免 SSE 流出现重复内容。
        if ("assistant".equals(event.type()) && event.message() != null) {
            if (state.sawStreamEvent) {
                return Collections.emptyList();
            }
            List<MessagesEvent> projected = new ArrayList<>(8);
            if (!state.sawMessageStart) {
                projected.add(new MessagesEvent("message_start", JSON.toJSONString(buildSyntheticMessageStart())));
                state.sawMessageStart = true;
            }
            JSONArray contentBlocks = event.message().getJSONArray("content");
            if (contentBlocks == null || contentBlocks.isEmpty()) {
                return projected;
            }
            for (int i = 0; i < contentBlocks.size(); i++) {
                JSONObject block = contentBlocks.getJSONObject(i);
                if (block == null) {
                    continue;
                }
                String blockType = block.getString("type");
                if (blockType == null) {
                    continue;
                }
                int blockIndex = state.nextBlockIndex++;
                switch (blockType) {
                    case "text" -> {
                        Map<String, Object> startBlock = new LinkedHashMap<>();
                        startBlock.put("type", "text");
                        startBlock.put("text", "");
                        Map<String, Object> startData = new LinkedHashMap<>();
                        startData.put("type", "content_block_start");
                        startData.put("index", blockIndex);
                        startData.put("content_block", startBlock);
                        projected.add(new MessagesEvent("content_block_start", JSON.toJSONString(startData)));
                        Map<String, Object> deltaMap = new LinkedHashMap<>();
                        deltaMap.put("type", "text_delta");
                        deltaMap.put("text", safeString(block.getString("text")));
                        Map<String, Object> deltaData = new LinkedHashMap<>();
                        deltaData.put("type", "content_block_delta");
                        deltaData.put("index", blockIndex);
                        deltaData.put("delta", deltaMap);
                        projected.add(new MessagesEvent("content_block_delta", JSON.toJSONString(deltaData)));
                        projected.add(new MessagesEvent("content_block_stop", JSON.toJSONString(
                                Map.of("type", "content_block_stop", "index", blockIndex))));
                    }
                    case "thinking" -> {
                        Map<String, Object> startBlock = new LinkedHashMap<>();
                        startBlock.put("type", "thinking");
                        Map<String, Object> startData = new LinkedHashMap<>();
                        startData.put("type", "content_block_start");
                        startData.put("index", blockIndex);
                        startData.put("content_block", startBlock);
                        projected.add(new MessagesEvent("content_block_start", JSON.toJSONString(startData)));
                        Map<String, Object> deltaMap = new LinkedHashMap<>();
                        deltaMap.put("type", "thinking_delta");
                        deltaMap.put("thinking", safeString(block.getString("thinking")));
                        Map<String, Object> deltaData = new LinkedHashMap<>();
                        deltaData.put("type", "content_block_delta");
                        deltaData.put("index", blockIndex);
                        deltaData.put("delta", deltaMap);
                        projected.add(new MessagesEvent("content_block_delta", JSON.toJSONString(deltaData)));
                        projected.add(new MessagesEvent("content_block_stop", JSON.toJSONString(
                                Map.of("type", "content_block_stop", "index", blockIndex))));
                    }
                    case "tool_use" -> {
                        Object input = block.get("input");
                        Map<String, Object> startBlock = new LinkedHashMap<>();
                        startBlock.put("type", "tool_use");
                        startBlock.put("id", safeString(block.getString("id")));
                        startBlock.put("name", safeString(block.getString("name")));
                        startBlock.put("input", input == null ? Map.of() : input);
                        Map<String, Object> startData = new LinkedHashMap<>();
                        startData.put("type", "content_block_start");
                        startData.put("index", blockIndex);
                        startData.put("content_block", startBlock);
                        projected.add(new MessagesEvent("content_block_start", JSON.toJSONString(startData)));
                        String inputJson = input == null ? "" : JSON.toJSONString(input);
                        Map<String, Object> deltaMap = new LinkedHashMap<>();
                        deltaMap.put("type", "input_json_delta");
                        deltaMap.put("partial_json", inputJson);
                        Map<String, Object> deltaData = new LinkedHashMap<>();
                        deltaData.put("type", "content_block_delta");
                        deltaData.put("index", blockIndex);
                        deltaData.put("delta", deltaMap);
                        projected.add(new MessagesEvent("content_block_delta", JSON.toJSONString(deltaData)));
                        projected.add(new MessagesEvent("content_block_stop", JSON.toJSONString(
                                Map.of("type", "content_block_stop", "index", blockIndex))));
                    }
                    default -> {
                        // Unknown block type, skip
                    }
                }
            }
            return projected;
        }

        // --- User events: extract tool_result blocks ---
        if ("user".equals(event.type()) && event.message() != null) {
            return projectToolResultEvents(event.message());
        }

        // --- Result event: signal end of turn ---
        if (!"result".equals(event.type())) {
            return Collections.emptyList();
        }

        if (Boolean.TRUE.equals(event.isError())) {
            return List.of(new MessagesEvent("error", JSON.toJSONString(Map.of(
                    "type", "error",
                    "error", buildErrorBody(event)
            ))));
        }

        List<MessagesEvent> projected = new ArrayList<>(3);
        if (!state.sawMessageStop) {
            if (!state.sawMessageDelta) {
                projected.add(new MessagesEvent("message_delta", JSON.toJSONString(buildSyntheticMessageDelta())));
                state.sawMessageDelta = true;
            }
            projected.add(new MessagesEvent("message_stop", JSON.toJSONString(Map.of("type", "message_stop"))));
            state.sawMessageStop = true;
        }
        projected.add(new MessagesEvent("turn_stop", "{}"));
        return projected;
    }

    private List<MessagesEvent> projectToolResultEvents(JSONObject message) {
        JSONArray content = message.getJSONArray("content");
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessagesEvent> projected = new ArrayList<>();
        for (int i = 0; i < content.size(); i++) {
            JSONObject block = content.getJSONObject(i);
            if (block == null || !"tool_result".equals(block.getString("type"))) {
                continue;
            }
            JSONObject payload = new JSONObject();
            payload.put("toolUseId", block.getString("tool_use_id"));
            payload.put("content", AgentMessageBlockPayloads.normalizeAnthropicToolResultContent(block.get("content")));
            payload.put("isError", Boolean.TRUE.equals(block.getBoolean("is_error")));
            projected.add(new MessagesEvent("tool_result", JSON.toJSONString(payload)));
        }
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

    private Map<String, Object> buildSyntheticMessageStart() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("type", "message_start");
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "message");
        message.put("role", "assistant");
        message.put("content", List.of());
        body.put("message", message);
        return body;
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

    private String safeString(String value) {
        return value == null ? "" : value;
    }

    private static final class ProjectionState {
        private boolean sawMessageStart;
        private boolean sawMessageDelta;
        private boolean sawMessageStop;
        private boolean sawStreamEvent;
        private int nextBlockIndex;
    }
}
