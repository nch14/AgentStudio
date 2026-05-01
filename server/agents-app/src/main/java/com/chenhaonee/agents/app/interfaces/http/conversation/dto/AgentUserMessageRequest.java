package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 产品级用户消息发送请求。
 */
public record AgentUserMessageRequest(
        @Schema(description = "用户输入内容", example = "狄仁杰的故事你知道多少？")
        @NotBlank(message = "content 不能为空")
        String content
) {
}
