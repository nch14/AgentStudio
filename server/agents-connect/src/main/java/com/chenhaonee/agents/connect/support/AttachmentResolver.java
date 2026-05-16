package com.chenhaonee.agents.connect.support;

import com.alibaba.fastjson2.JSONObject;

import java.util.List;

/**
 * 附件解析与翻译能力。
 * <p>
 * 负责把 chat / task 的附件元数据 (ossKey + mime + filename + size) 转换为
 * provider 可消费的形态。所有 OSS 字节读取统一收口在本接口的实现内。
 */
public interface AttachmentResolver {

    /**
     * 校验一组附件 ossKey 是否归属于指定 agent。
     * 当前 keyspace 约定 ossKey 以 {@code <agentCode>/} 开头。
     */
    void validateOwnership(String agentCode, List<AttachmentRef> refs);

    /**
     * 拉取 OSS 字节，可能命中进程内缓存。
     */
    byte[] fetchBytes(String ossKey);

    /**
     * 拼接 OSS 对象的对外可访问 URL（可能签名）。落库时写入 IMAGE / DOCUMENT block 的 url 字段。
     */
    String publicUrl(String ossKey);

    /**
     * 渲染为 Anthropic Messages 协议风格的 content block。
     * <ul>
     *     <li>image/* → {@code {type:"image", source:{type:"base64", ...}}}</li>
     *     <li>application/pdf → {@code {type:"document", source:{type:"base64", ...}}}</li>
     *     <li>text/plain | text/markdown → {@code {type:"document", source:{type:"text", ...}}}</li>
     * </ul>
     */
    JSONObject toAnthropicBlock(AttachmentRef ref);

    /**
     * 附件元数据引用。
     */
    record AttachmentRef(String ossKey, String mime, String filename, long size) {
    }
}
