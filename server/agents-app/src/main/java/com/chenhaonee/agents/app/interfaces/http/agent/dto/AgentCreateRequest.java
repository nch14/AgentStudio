package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * 创建 Agent 请求。
 */
public record AgentCreateRequest(
        @Schema(description = "名称", example = "新闻助手")
        @NotBlank(message = "名称不能为空")
        String name,

        @Schema(description = "职责描述", example = "负责每日新闻摘要和热点推送")
        String responsibility,

        @Schema(description = "提供者类型", example = "CLAUDE_CODE")
        @NotBlank(message = "提供者类型不能为空")
        String provider,

        @Schema(description = "提供者配置")
        Map<String, String> providerConfig
) {
}
