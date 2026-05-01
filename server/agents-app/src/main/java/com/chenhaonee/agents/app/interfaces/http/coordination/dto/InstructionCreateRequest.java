package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "创建指令请求")
public record InstructionCreateRequest(
        @Schema(description = "关联的回合编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "回合编码不能为空")
        String turnCode,

        @Schema(description = "指令内容", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "指令内容不能为空")
        String content
) {
}
