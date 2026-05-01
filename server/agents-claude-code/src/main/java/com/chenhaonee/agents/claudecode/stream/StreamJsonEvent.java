package com.chenhaonee.agents.claudecode.stream;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.annotation.JSONField;

import java.util.List;

/**
 * Claude Code stream-json 输出行反序列化对象。
 * <p>
 * CLI 通过 --output-format stream-json 逐行输出 JSON 事件。
 */
public record StreamJsonEvent(
        String type,
        String subtype,
        String content,
        String tool_name,
        String tool_input,
        String result,
        JSONObject event,
        JSONObject message,
        String session_id,
        String error,
        @JSONField(name = "is_error") Boolean isError,
        List<String> errors
) {

    public String streamEventType() {
        return event == null ? null : event.getString("type");
    }

    public JSONObject streamDelta() {
        return event == null ? null : event.getJSONObject("delta");
    }

    public String streamDeltaType() {
        JSONObject delta = streamDelta();
        return delta == null ? null : delta.getString("type");
    }

    public String streamDeltaText() {
        JSONObject delta = streamDelta();
        return delta == null ? null : delta.getString("text");
    }

    public String streamDeltaThinking() {
        JSONObject delta = streamDelta();
        return delta == null ? null : delta.getString("thinking");
    }

    public String assistantMessageText() {
        return extractMessageContentField("text");
    }

    public String assistantMessageThinking() {
        return extractMessageContentField("thinking");
    }

    private String extractMessageContentField(String fieldName) {
        if (message == null) {
            return null;
        }
        JSONArray contentArray = message.getJSONArray("content");
        if (contentArray == null || contentArray.isEmpty()) {
            return null;
        }
        for (int i = 0; i < contentArray.size(); i++) {
            JSONObject block = contentArray.getJSONObject(i);
            if (block == null) {
                continue;
            }
            String value = block.getString(fieldName);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }
}
