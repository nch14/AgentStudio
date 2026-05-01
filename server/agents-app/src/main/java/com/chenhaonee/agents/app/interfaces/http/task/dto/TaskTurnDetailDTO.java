package com.chenhaonee.agents.app.interfaces.http.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 任务回合详情响应 DTO。
 */
public record TaskTurnDetailDTO(
        @Schema(description = "回合编码") String turnCode,
        @Schema(description = "所属任务编码") String taskCode,
        @Schema(description = "回合序号") int turnNo,
        @Schema(description = "运行状态") String runStatus,
        @Schema(description = "回合结束时项目进度（0-100）") int progress,
        @Schema(description = "开始时间") String startedAt,
        @Schema(description = "完成时间") String finishedAt,
        @Schema(description = "最终摘要") String finalSummary,
        @Schema(description = "最终详情") String finalDetail
) {
}
