package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent 会话消息响应 DTO。
 */
public record AgentSessionMessageDTO(
        @Schema(description = "消息编码") String code,
        @Schema(description = "角色") String role,
        @Schema(description = "协议类型") String protocolType,
        @Schema(description = "消息状态") String status,
        @Schema(description = "标准消息 JSON") String payloadJson,
        @Schema(description = "错误消息 JSON") String errorPayloadJson,
        @Schema(description = "标准外部消息 ID") String externalMessageId,
        @Schema(description = "消息序号") int messageIndex
) {
}
