package com.chenhaonee.agents.connect.support;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.support.AttachmentResolver.AttachmentRef;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * {@link AttachmentRef} 与 fastjson2 之间的统一序列化 / 反序列化工具。
 * <p>
 * App 层把 AttachmentRequest 序列化成顶层 _attachments 数组下传 SPI 时使用 toJsonArray；
 * Provider 层把 _attachments 或 attachmentsJson 解回 List 时使用 parse。
 */
public final class AttachmentRefs {

    private AttachmentRefs() {
    }

    public static JSONArray toJsonArray(List<AttachmentRef> refs) {
        JSONArray array = new JSONArray();
        if (refs == null || refs.isEmpty()) {
            return array;
        }
        for (AttachmentRef ref : refs) {
            JSONObject meta = new JSONObject();
            meta.put("ossKey", ref.ossKey());
            meta.put("mime", ref.mime());
            meta.put("filename", ref.filename());
            meta.put("size", ref.size());
            array.add(meta);
        }
        return array;
    }

    public static String toJsonString(List<AttachmentRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return null;
        }
        return JSON.toJSONString(toJsonArray(refs));
    }

    public static List<AttachmentRef> parse(JSONArray array) {
        if (array == null || array.isEmpty()) {
            return List.of();
        }
        List<AttachmentRef> refs = new ArrayList<>(array.size());
        for (int i = 0; i < array.size(); i++) {
            JSONObject obj = array.getJSONObject(i);
            if (obj == null) {
                continue;
            }
            refs.add(new AttachmentRef(
                    obj.getString("ossKey"),
                    obj.getString("mime"),
                    obj.getString("filename"),
                    obj.getLongValue("size")
            ));
        }
        return refs;
    }

    public static List<AttachmentRef> parse(String json) {
        if (StringUtils.isBlank(json)) {
            return List.of();
        }
        return parse(JSON.parseArray(json));
    }
}
