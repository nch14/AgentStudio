package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "回答结果响应 DTO")
public record AnswerDTO(
        @Schema(description = "对应的问题编码") String questionCode,
        @Schema(description = "选择的选项") String selectedOption,
        @Schema(description = "输入的内容") String userInput
) {
}
