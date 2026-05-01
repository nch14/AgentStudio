package com.chenhaonee.agents.connect.mcp.tool;

import com.chenhaonee.agents.connect.capability.TurnEndReport;
import com.chenhaonee.agents.connect.capability.TurnEndReportRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 提供给大语言模型声明 turn 终态的工具。
 */
@Component
@RequiredArgsConstructor
public class TurnReportTools {

    private final TurnEndReportRegistry turnEndReportRegistry;

    @Tool(name = "reportTurnEnd",
            description = "声明本 turn 应该结束并提交结论。progress 为对当前 task 整体进度（0-100）的自评，不评估则不传；summary 必填，detail 可选。")
    public String reportTurnEnd(
            @ToolParam(description = "必填，当前 turnCode") String turnCode,
            @ToolParam(description = "可选，task 整体进度 0-100；不评估则不传", required = false) Integer progress,
            @ToolParam(description = "必填，本轮简明结论") String summary,
            @ToolParam(description = "可选，详细信息", required = false) String detail) {
        if (turnCode == null || turnCode.isBlank()) {
            throw new IllegalArgumentException("turnCode must not be blank");
        }
        if (summary == null || summary.isBlank()) {
            throw new IllegalArgumentException("summary must not be blank");
        }
        if (progress != null && (progress < 0 || progress > 100)) {
            throw new IllegalArgumentException("progress must be between 0 and 100, but was: " + progress);
        }
        turnEndReportRegistry.report(turnCode, new TurnEndReport(progress, summary, detail));
        return "ok";
    }
}
