package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "问题集响应 DTO")
public record QuestionsDTO(
        @Schema(description = "问题集唯一标识") String code,
        @Schema(description = "是否已解决") boolean resolved,
        @Schema(description = "开启时间") String openedAt,
        @Schema(description = "解决时间") String resolvedAt,
        @Schema(description = "问题列表") List<QuestionDTO> questions,
        @Schema(description = "回答列表") List<AnswerDTO> answers
) {
}
