package com.chenhaonee.agents.app.interfaces.http.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新任务请求。
 */
public record TaskUpdateRequest(
        @Schema(description = "标题", example = "搜索最新的 AI 新闻")
        String title,

        @Schema(description = "任务描述", example = "搜索并总结今天关于人工智能的最新新闻")
        String description
) {
}
