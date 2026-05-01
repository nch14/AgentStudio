package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "问题响应 DTO")
public record QuestionDTO(
        @Schema(description = "问题唯一编码") String code,
        @Schema(description = "问题内容") String text,
        @Schema(description = "预期选项列表，可为空") List<String> options
) {
}
