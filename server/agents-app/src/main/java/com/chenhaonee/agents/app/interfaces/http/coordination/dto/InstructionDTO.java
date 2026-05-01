package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "指令响应 DTO")
public record InstructionDTO(
        @Schema(description = "指令编码") String code,
        @Schema(description = "关联的任务编码") String taskCode,
        @Schema(description = "关联的回合编码") String turnCode,
        @Schema(description = "指令内容") String content,
        @Schema(description = "状态") String status,
        @Schema(description = "创建时间") String createdAt
) {
}
