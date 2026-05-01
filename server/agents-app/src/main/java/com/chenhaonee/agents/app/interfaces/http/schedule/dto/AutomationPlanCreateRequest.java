package com.chenhaonee.agents.app.interfaces.http.schedule.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 创建自动化计划请求。
 */
public record AutomationPlanCreateRequest(
        @Schema(description = "计划名称", example = "每日晨报")
        @NotBlank(message = "计划名称不能为空")
        String name,

        @Schema(description = "关联 Agent 编码", example = "agent-xxx")
        @NotBlank(message = "Agent 编码不能为空")
        String agentCode,

        @Schema(description = "任务指令", example = "搜索并总结今天的新闻")
        @NotBlank(message = "任务指令不能为空")
        String instruction,

        @Schema(description = "调度规则（cron 表达式）", example = "0 8 * * *")
        @NotBlank(message = "调度规则不能为空")
        String scheduleRule,

        @Schema(description = "交付方式（PUSH 或 CONVERSATION）", example = "PUSH")
        @NotBlank(message = "交付方式不能为空")
        String deliveryMode
) {
}
