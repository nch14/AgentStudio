package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Agent 详情响应 DTO。
 */
public record AgentDetailDTO(
        @Schema(description = "Agent 编码") String code,
        @Schema(description = "名称") String name,
        @Schema(description = "职责描述") String responsibility,
        @Schema(description = "提供者类型") String provider,
        @Schema(description = "状态") String status,
        @Schema(description = "提供者配置") java.util.Map<String, String> providerConfig,
        @Schema(description = "创建时间") String createTime,
        @Schema(description = "更新时间") String updateTime
) {
}
