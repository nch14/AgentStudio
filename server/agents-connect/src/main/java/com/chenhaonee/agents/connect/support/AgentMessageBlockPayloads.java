package com.chenhaonee.agents.connect.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;

/**
 * AgentMessage block payload 构造工具。
 */
public final class AgentMessageBlockPayloads {

    private AgentMessageBlockPayloads() {
    }

    public static String text(String text) {
        JSONObject payload = new JSONObject();
        payload.put("text", text);
        return JSON.toJSONString(payload);
    }

    public static String thinking(String thinking, String signature) {
        JSONObject payload = new JSONObject();
        payload.put("thinking", thinking);
        if (signature != null) {
            payload.put("signature", signature);
        }
        return JSON.toJSONString(payload);
    }

    public static String toolUse(String toolUseId, String name, Object input) {
        JSONObject payload = new JSONObject();
        payload.put("toolUseId", toolUseId);
        payload.put("name", name);
        payload.put("input", input);
        return JSON.toJSONString(payload);
    }

    public static String toolResult(String toolUseId, Object content, boolean isError) {
        JSONObject payload = new JSONObject();
        payload.put("toolUseId", toolUseId);
        payload.put("content", content);
        payload.put("isError", isError);
        return JSON.toJSONString(payload);
    }

    /**
     * 图片附件 payload。仅引用 OSS，不写入 base64 字节。
     */
    public static String image(String ossKey, String mime, String filename, long size, String url) {
        return JSON.toJSONString(attachmentPayload(ossKey, mime, filename, size, url));
    }

    /**
     * 文档附件 payload。仅引用 OSS，不写入 base64 字节。
     */
    public static String document(String ossKey, String mime, String filename, long size, String url) {
        return JSON.toJSONString(attachmentPayload(ossKey, mime, filename, size, url));
    }

    private static JSONObject attachmentPayload(String ossKey, String mime, String filename, long size, String url) {
        JSONObject payload = new JSONObject();
        payload.put("ossKey", ossKey);
        payload.put("mime", mime);
        payload.put("filename", filename);
        payload.put("size", size);
        if (url != null) {
            payload.put("url", url);
        }
        return payload;
    }

    public static Object parseJsonObjectOrRaw(String raw) {
        if (raw == null) {
            return null;
        }
        try {
            return JSON.parseObject(raw);
        } catch (Exception ignored) {
            JSONObject fallback = new JSONObject();
            fallback.put("_raw", raw);
            return fallback;
        }
    }

    public static Object normalizeAnthropicToolResultContent(Object content) {
        if (content instanceof JSONArray || content instanceof JSONObject || content instanceof String) {
            return content;
        }
        return content == null ? "" : content;
    }
}
