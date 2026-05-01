package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;

/**
 * 将 Anthropic Messages 流式事件聚合为最终 message JSON。
 */
class AnthropicMessageAssembler {

    private JSONObject finalMessage;
    private final Map<Integer, StringBuilder> toolInputBuffers = new HashMap<>();

    public void onEvent(MessagesEvent event) {
        if (event == null || StringUtils.isBlank(event.dataJson())) {
            return;
        }
        JSONObject data = JSON.parseObject(event.dataJson());
        if (data == null) {
            return;
        }
        switch (event.eventType()) {
            case "message_start" -> onMessageStart(data);
            case "content_block_start" -> onContentBlockStart(data);
            case "content_block_delta" -> onContentBlockDelta(data);
            case "content_block_stop" -> onContentBlockStop(data);
            case "message_delta" -> onMessageDelta(data);
            default -> {
            }
        }
    }

    public String toFinalMessageJson() {
        if (finalMessage == null) {
            return null;
        }
        return JSON.toJSONString(finalMessage);
    }

    private void onMessageStart(JSONObject data) {
        JSONObject message = data.getJSONObject("message");
        if (message == null) {
            return;
        }
        finalMessage = JSON.parseObject(JSON.toJSONString(message));
        if (finalMessage.getJSONArray("content") == null) {
            finalMessage.put("content", new JSONArray());
        }
    }

    private void onContentBlockStart(JSONObject data) {
        JSONObject contentBlock = data.getJSONObject("content_block");
        if (contentBlock == null) {
            return;
        }
        JSONArray content = ensureContentArray();
        int index = data.getIntValue("index");
        ensureArraySize(content, index);
        content.set(index, JSON.parseObject(JSON.toJSONString(contentBlock)));
    }

    private void onContentBlockDelta(JSONObject data) {
        JSONObject delta = data.getJSONObject("delta");
        if (delta == null) {
            return;
        }
        JSONObject block = getContentBlock(data.getIntValue("index"));
        if (block == null) {
            return;
        }
        switch (delta.getString("type")) {
            case "text_delta" -> block.put("text", append(block.getString("text"), delta.getString("text")));
            case "thinking_delta" -> block.put("thinking", append(block.getString("thinking"), delta.getString("thinking")));
            case "signature_delta" -> block.put("signature", delta.getString("signature"));
            case "input_json_delta" -> toolInputBuffers
                    .computeIfAbsent(data.getIntValue("index"), key -> new StringBuilder())
                    .append(delta.getString("partial_json"));
            default -> {
            }
        }
    }

    private void onContentBlockStop(JSONObject data) {
        Integer index = data.getInteger("index");
        if (index == null) {
            return;
        }
        StringBuilder partialJson = toolInputBuffers.remove(index);
        if (partialJson == null) {
            return;
        }
        JSONObject block = getContentBlock(index);
        if (block == null) {
            return;
        }
        String input = partialJson.toString();
        if (StringUtils.isBlank(input)) {
            return;
        }
        block.put("input", JSON.parseObject(input));
    }

    private void onMessageDelta(JSONObject data) {
        if (finalMessage == null) {
            return;
        }
        JSONObject delta = data.getJSONObject("delta");
        if (delta != null) {
            for (Map.Entry<String, Object> entry : delta.entrySet()) {
                finalMessage.put(entry.getKey(), entry.getValue());
            }
        }
        JSONObject usage = data.getJSONObject("usage");
        if (usage != null) {
            finalMessage.put("usage", usage);
        }
    }

    private JSONArray ensureContentArray() {
        if (finalMessage == null) {
            finalMessage = new JSONObject();
        }
        JSONArray content = finalMessage.getJSONArray("content");
        if (content == null) {
            content = new JSONArray();
            finalMessage.put("content", content);
        }
        return content;
    }

    private JSONObject getContentBlock(int index) {
        JSONArray content = ensureContentArray();
        if (index < 0 || index >= content.size()) {
            return null;
        }
        return content.getJSONObject(index);
    }

    private void ensureArraySize(JSONArray array, int index) {
        while (array.size() <= index) {
            array.add(null);
        }
    }

    private String append(String current, String delta) {
        return (current == null ? "" : current) + (delta == null ? "" : delta);
    }
}
