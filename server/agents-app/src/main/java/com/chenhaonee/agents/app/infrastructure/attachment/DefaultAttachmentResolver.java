package com.chenhaonee.agents.app.infrastructure.attachment;

import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.common.oss.OssObjectService;
import com.chenhaonee.agents.common.utils.UrlUtils;
import com.chenhaonee.agents.connect.support.AttachmentResolver;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Set;

/**
 * AttachmentResolver 默认实现：
 * <ul>
 *     <li>OSS 字节读取：通过 {@link OssObjectService}，命中进程内 LRU 缓存。</li>
 *     <li>归属校验：当前 keyspace 约定 ossKey 必须以 {@code <agentCode>/} 开头。</li>
 *     <li>Anthropic content block 翻译：图片 / PDF 走 base64，txt / md 走 text source。</li>
 * </ul>
 */
@Component
@Slf4j
public class DefaultAttachmentResolver implements AttachmentResolver {

    private static final Set<String> TEXT_DOCUMENT_MIMES = Set.of("text/plain", "text/markdown");

    private final OssObjectService ossObjectService;
    private final String ossPublicUrl;
    private final String bucket;
    private final Duration presignDuration;
    private final Cache<String, byte[]> bytesCache = Caffeine.newBuilder()
            .maximumWeight(256L * 1024 * 1024)
            .weigher((String key, byte[] value) -> value.length)
            .expireAfterWrite(Duration.ofMinutes(10))
            .build();

    public DefaultAttachmentResolver(
            OssObjectService ossObjectService,
            @Value("${oss.public-url:${oss.url:}}") String ossPublicUrl,
            @Value("${oss.bucket:agents}") String bucket,
            @Value("${oss.presign-expire-minutes:10}") long presignExpireMinutes
    ) {
        this.ossObjectService = ossObjectService;
        this.ossPublicUrl = ossPublicUrl;
        this.bucket = bucket;
        this.presignDuration = Duration.ofMinutes(Math.max(1, Math.min(presignExpireMinutes, 7 * 24 * 60)));
    }

    @Override
    public void validateOwnership(String agentCode, List<AttachmentRef> refs) {
        if (refs == null || refs.isEmpty()) {
            return;
        }
        if (StringUtils.isBlank(agentCode)) {
            throw new IllegalArgumentException("agentCode 不能为空");
        }
        String prefix = agentCode + "/";
        for (AttachmentRef ref : refs) {
            if (ref == null || StringUtils.isBlank(ref.ossKey())) {
                throw new IllegalArgumentException("attachment ossKey 不能为空");
            }
            if (!ref.ossKey().startsWith(prefix)) {
                throw new IllegalArgumentException("attachment 归属校验失败, ossKey=" + ref.ossKey()
                        + ", agentCode=" + agentCode);
            }
        }
    }

    @Override
    public byte[] fetchBytes(String ossKey) {
        if (StringUtils.isBlank(ossKey)) {
            throw new IllegalArgumentException("ossKey 不能为空");
        }
        return bytesCache.get(ossKey, ossObjectService::readAsBytes);
    }

    @Override
    public String publicUrl(String ossKey) {
        if (StringUtils.isBlank(ossKey)) {
            return null;
        }
        try {
            return ossObjectService.presignGetObject(ossKey, presignDuration).toString();
        } catch (Exception e) {
            log.warn("生成 presigned URL 失败, key={}, 回退为直接 URL", ossKey, e);
            if (StringUtils.isBlank(ossPublicUrl)) {
                return ossKey;
            }
            return UrlUtils.joinHttpRootAndPath(ossPublicUrl, "/" + bucket + "/" + ossKey);
        }
    }

    @Override
    public JSONObject toAnthropicBlock(AttachmentRef ref) {
        if (ref == null) {
            throw new IllegalArgumentException("attachment ref 不能为空");
        }
        String mime = StringUtils.lowerCase(ref.mime());
        if (StringUtils.startsWith(mime, "image/")) {
            return imageBlock(ref, mime);
        }
        if (TEXT_DOCUMENT_MIMES.contains(mime)) {
            return textDocumentBlock(ref, mime);
        }
        if ("application/pdf".equals(mime)) {
            return base64DocumentBlock(ref, mime);
        }
        throw new IllegalArgumentException("不支持的附件 mime: " + ref.mime());
    }

    private JSONObject imageBlock(AttachmentRef ref, String mime) {
        JSONObject source = new JSONObject();
        source.put("type", "base64");
        source.put("media_type", mime);
        source.put("data", Base64.getEncoder().encodeToString(fetchBytes(ref.ossKey())));

        JSONObject block = new JSONObject();
        block.put("type", "image");
        block.put("source", source);
        return block;
    }

    private JSONObject base64DocumentBlock(AttachmentRef ref, String mime) {
        JSONObject source = new JSONObject();
        source.put("type", "base64");
        source.put("media_type", mime);
        source.put("data", Base64.getEncoder().encodeToString(fetchBytes(ref.ossKey())));

        JSONObject block = new JSONObject();
        block.put("type", "document");
        block.put("source", source);
        if (StringUtils.isNotBlank(ref.filename())) {
            block.put("title", ref.filename());
        }
        return block;
    }

    private JSONObject textDocumentBlock(AttachmentRef ref, String mime) {
        JSONObject source = new JSONObject();
        source.put("type", "text");
        source.put("media_type", mime);
        source.put("data", new String(fetchBytes(ref.ossKey()), StandardCharsets.UTF_8));

        JSONObject block = new JSONObject();
        block.put("type", "document");
        block.put("source", source);
        if (StringUtils.isNotBlank(ref.filename())) {
            block.put("title", ref.filename());
        }
        return block;
    }
}
