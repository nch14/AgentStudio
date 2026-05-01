package com.chenhaonee.agents.app.interfaces.http.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务列表项响应 DTO。
 */
public record TaskListItemDTO(
        @Schema(description = "任务编码") String taskCode,
        @Schema(description = "标题") String title,
        @Schema(description = "Agent 编码") String agentCode,
        @Schema(description = "创建来源") String source,
        @Schema(description = "任务状态") String status,
        @Schema(description = "完成进度") int progress,
        @Schema(description = "完成时间") String finishedAt,
        @Schema(description = "创建时间") String createTime,
        @Schema(description = "更新时间") String updateTime
) {
}
