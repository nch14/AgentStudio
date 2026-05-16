package com.chenhaonee.agents.app.application.conversation;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.interfaces.http.common.dto.AttachmentRequest;
import com.chenhaonee.agents.connect.support.AttachmentResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 把"用户文本 + 附件元数据"翻译为 Anthropic Messages user.content 数组。
 * 字节读取与 base64 编码统一收口在 {@link AttachmentResolver}。
 */
@Component
public class AnthropicContentBlockBuilder {

    private final AttachmentResolver attachmentResolver;

    public AnthropicContentBlockBuilder(AttachmentResolver attachmentResolver) {
        this.attachmentResolver = attachmentResolver;
    }

    public JSONArray buildUserContent(String text, List<AttachmentRequest> attachments) {
        JSONArray content = new JSONArray();
        if (StringUtils.isNotBlank(text)) {
            JSONObject textBlock = new JSONObject();
            textBlock.put("type", "text");
            textBlock.put("text", text);
            content.add(textBlock);
        }
        if (attachments == null || attachments.isEmpty()) {
            return content;
        }
        for (AttachmentRequest attachment : attachments) {
            content.add(attachmentResolver.toAnthropicBlock(toRef(attachment)));
        }
        return content;
    }

    /**
     * 把附件元数据序列化为 requestJson 顶层 _attachments 数组，供 provider 层重建 input items。
     */
    public JSONArray buildAttachmentsMeta(List<AttachmentRequest> attachments) {
        JSONArray result = new JSONArray();
        if (attachments == null || attachments.isEmpty()) {
            return result;
        }
        for (AttachmentRequest attachment : attachments) {
            JSONObject meta = new JSONObject();
            meta.put("ossKey", attachment.ossKey());
            meta.put("mime", attachment.mime());
            meta.put("filename", attachment.filename());
            meta.put("size", attachment.size());
            result.add(meta);
        }
        return result;
    }

    private AttachmentResolver.AttachmentRef toRef(AttachmentRequest attachment) {
        return new AttachmentResolver.AttachmentRef(
                attachment.ossKey(), attachment.mime(), attachment.filename(), attachment.size());
    }
}
