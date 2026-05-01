package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 单个 Provider 支持的能力声明 DTO。
 */
public record ProviderCapabilityDTO(
        @Schema(description = "Provider 类型") String provider,
        @Schema(description = "是否支持对话型 API") boolean supportChat,
        @Schema(description = "是否支持 Messages API") boolean supportMessages,
        @Schema(description = "是否支持任务型 API") boolean supportTask
) {
}
