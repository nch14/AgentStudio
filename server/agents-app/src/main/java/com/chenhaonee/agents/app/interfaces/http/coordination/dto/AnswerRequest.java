package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "单项回答")
public record AnswerRequest(
        @Schema(description = "关联的问题编码", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "问题编码不能为空")
        String questionCode,

        @Schema(description = "选择的选项（如果有）")
        String selectedOption,

        @Schema(description = "用户输入的内容")
        String userInput
) {
}
