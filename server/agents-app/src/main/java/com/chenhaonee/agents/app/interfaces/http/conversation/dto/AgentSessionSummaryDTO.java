package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 会话摘要响应 DTO。
 */
public record AgentSessionSummaryDTO(
        @Schema(description = "会话标题") String title,
        @Schema(description = "最近一条消息预览") String lastMessageSnippet,
        @Schema(description = "对话轮次数") int turnCount
) {
}
