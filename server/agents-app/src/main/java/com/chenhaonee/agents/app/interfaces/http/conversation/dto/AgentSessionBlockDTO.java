package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent 会话 turn 内的 block 响应 DTO。
 */
public record AgentSessionBlockDTO(
        @Schema(description = "block 编码") String code,
        @Schema(description = "角色：USER / ASSISTANT / TOOL / SYSTEM") String role,
        @Schema(description = "block 类型：TEXT / THINKING / TOOL_USE / TOOL_RESULT / IMAGE / DOCUMENT / TURN_START / TURN_STOP") String type,
        @Schema(description = "消息序号") int messageIndex,
        @Schema(description = "block 载荷，类型随 type 不同而不同") Object payload,
        @Schema(description = "错误载荷，仅 status=CANCELLED/FAILED 时填充") Object errorPayload,
        @Schema(description = "外部消息 ID（Anthropic message id）") String externalMessageId
) {
}
