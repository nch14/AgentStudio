package com.chenhaonee.agents.app.application.conversation;

import com.chenhaonee.agents.app.interfaces.http.common.dto.AttachmentRequest;
import com.chenhaonee.agents.connect.support.AttachmentResolver;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 附件请求体校验：mime 白名单、单文件大小、单条消息数量上限、ossKey 归属。
 */
@Component
public class AttachmentValidator {

    private static final Set<String> IMAGE_MIMES = Set.of("image/png", "image/jpeg", "image/gif", "image/webp");
    private static final Set<String> DOCUMENT_MIMES = Set.of("application/pdf", "text/plain", "text/markdown");
    private static final long IMAGE_MAX_BYTES = 10L * 1024 * 1024;
    private static final long DOCUMENT_MAX_BYTES = 32L * 1024 * 1024;
    private static final int MAX_TOTAL = 10;
    private static final int MAX_IMAGES = 8;
    private static final int MAX_DOCUMENTS = 2;

    private final AttachmentResolver attachmentResolver;

    public AttachmentValidator(AttachmentResolver attachmentResolver) {
        this.attachmentResolver = attachmentResolver;
    }

    /**
     * 校验一组附件。空 / null 直接放行。
     */
    public void validate(String agentCode, List<AttachmentRequest> attachments) {
        if (attachments == null || attachments.isEmpty()) {
            return;
        }
        if (attachments.size() > MAX_TOTAL) {
            throw new IllegalArgumentException("附件数量超过单条消息上限 " + MAX_TOTAL);
        }

        int images = 0;
        int documents = 0;
        for (AttachmentRequest attachment : attachments) {
            if (attachment == null) {
                throw new IllegalArgumentException("附件不能为空对象");
            }
            if (StringUtils.isBlank(attachment.filename())) {
                throw new IllegalArgumentException("附件 filename 不能为空");
            }
            String mime = StringUtils.lowerCase(attachment.mime());
            if (IMAGE_MIMES.contains(mime)) {
                images++;
                requireSize(attachment, IMAGE_MAX_BYTES, "image");
            } else if (DOCUMENT_MIMES.contains(mime)) {
                documents++;
                requireSize(attachment, DOCUMENT_MAX_BYTES, "document");
            } else {
                throw new IllegalArgumentException("不支持的附件 mime: " + attachment.mime());
            }
        }
        if (images > MAX_IMAGES) {
            throw new IllegalArgumentException("图片附件数量超过单条消息上限 " + MAX_IMAGES);
        }
        if (documents > MAX_DOCUMENTS) {
            throw new IllegalArgumentException("文档附件数量超过单条消息上限 " + MAX_DOCUMENTS);
        }

        attachmentResolver.validateOwnership(agentCode, attachments.stream()
                .map(a -> new AttachmentResolver.AttachmentRef(a.ossKey(), a.mime(), a.filename(), a.size()))
                .toList());
    }

    private void requireSize(AttachmentRequest attachment, long max, String kind) {
        if (attachment.size() <= 0) {
            throw new IllegalArgumentException("附件 size 必须为正数: " + attachment.filename());
        }
        if (attachment.size() > max) {
            throw new IllegalArgumentException(kind + " 附件超过单文件上限 "
                    + (max / 1024 / 1024) + "MB: " + attachment.filename());
        }
    }
}
