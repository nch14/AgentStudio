package com.chenhaonee.agents.app.interfaces.http.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 自动化计划详情 DTO。
 */
public record AutomationPlanDetailDTO(
        @Schema(description = "计划编码") String planCode,
        @Schema(description = "计划名称") String name,
        @Schema(description = "关联 Agent 编码") String agentCode,
        @Schema(description = "任务指令") String instruction,
        @Schema(description = "调度规则（cron 表达式）") String scheduleRule,
        @Schema(description = "交付方式（PUSH/CONVERSATION）") String deliveryMode,
        @Schema(description = "状态（ENABLED/DISABLED）") String status,
        @Schema(description = "创建时间") String createTime,
        @Schema(description = "更新时间") String updateTime
) {
}
