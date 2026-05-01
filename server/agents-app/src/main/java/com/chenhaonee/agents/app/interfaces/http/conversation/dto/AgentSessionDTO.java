package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent 会话响应 DTO。
 */
public record AgentSessionDTO(
        @Schema(description = "会话编码") String code,
        @Schema(description = "标题") String title,
        @Schema(description = "关联的 Agent 编码") String agentCode,
        @Schema(description = "消息数量") int messageCount,
        @Schema(description = "最后消息时间") String lastMessageTime,
        @Schema(description = "是否已归档") boolean archived,
        @Schema(description = "创建时间") String createTime,
        @Schema(description = "更新时间") String updateTime
) {
}
