package com.chenhaonee.agents.app.interfaces.http.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * task 详情响应 DTO。
 */
public record TaskDetailDTO(
        @Schema(description = "任务编码") String taskCode,
        @Schema(description = "标题") String title,
        @Schema(description = "任务内容") String content,
        @Schema(description = "Agent 编码") String agentCode,
        @Schema(description = "关联会话编码") String sessionCode,
        @Schema(description = "创建来源") String source,
        @Schema(description = "来源实体编码，USER_CREATE 为 ownerCode，SCHEDULED_CREATE 为 planCode") String sourceRef,
        @Schema(description = "任务状态") String status,
        @Schema(description = "当前回合编码") String currentTurnCode,
        @Schema(description = "当前回合序号") Integer currentTurnNo,
        @Schema(description = "完成进度") int progress,
        @Schema(description = "结果摘要") String resultSummary,
        @Schema(description = "完成时间") String finishedAt
) {
}
