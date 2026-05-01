package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Map;

/**
 * 更新 Agent 请求。
 */
public record AgentUpdateRequest(
        @Schema(description = "名称", example = "新闻助手")
        String name,

        @Schema(description = "职责描述", example = "负责每日新闻摘要和热点推送")
        String responsibility,

        @Schema(description = "提供者配置")
        Map<String, String> providerConfig
) {
}
