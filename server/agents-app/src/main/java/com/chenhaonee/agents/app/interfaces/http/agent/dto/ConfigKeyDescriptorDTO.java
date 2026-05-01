package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 配置项描述符 DTO。
 */
public record ConfigKeyDescriptorDTO(
        @Schema(description = "提供者类型") String provider,
        @Schema(description = "配置 key") String key,
        @Schema(description = "展示名称") String displayName,
        @Schema(description = "描述") String description,
        @Schema(description = "是否必填") boolean required,
        @Schema(description = "默认值") String defaultValue
) {
}
