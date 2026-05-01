package com.chenhaonee.agents.app.interfaces.http.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 更新自动化计划请求。
 */
public record AutomationPlanUpdateRequest(
        @Schema(description = "计划名称", example = "每日晨报")
        String name,

        @Schema(description = "任务指令", example = "搜索并总结今天的新闻")
        String instruction,

        @Schema(description = "调度规则（cron 表达式）", example = "0 8 * * *")
        String scheduleRule,

        @Schema(description = "交付方式（PUSH 或 CONVERSATION）", example = "PUSH")
        String deliveryMode
) {
}
