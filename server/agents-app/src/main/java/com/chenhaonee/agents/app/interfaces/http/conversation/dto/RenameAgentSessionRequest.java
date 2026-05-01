package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 重命名 Agent 会话请求。
 */
public record RenameAgentSessionRequest(
        @Schema(description = "新标题", example = "今日新闻摘要")
        @NotBlank(message = "标题不能为空")
        String title
) {
}
