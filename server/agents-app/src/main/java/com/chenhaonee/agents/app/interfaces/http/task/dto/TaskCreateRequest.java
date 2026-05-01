package com.chenhaonee.agents.app.interfaces.http.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建任务请求。
 */
public record TaskCreateRequest(
        @Schema(description = "标题", example = "搜索最新的 AI 新闻")
        @NotBlank(message = "标题不能为空")
        String title,

        @Schema(description = "Agent 编码", example = "agent-xxx")
        @NotBlank(message = "Agent 编码不能为空")
        String agentCode,

        @Schema(description = "任务描述", example = "搜索并总结今天关于人工智能的最新新闻")
        @NotBlank(message = "描述不能为空")
        String description
) {
}
