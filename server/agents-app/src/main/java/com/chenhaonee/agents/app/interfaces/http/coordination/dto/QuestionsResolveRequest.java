package com.chenhaonee.agents.app.interfaces.http.coordination.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "回复问题集请求")
public record QuestionsResolveRequest(
        @Schema(description = "回答列表", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotEmpty(message = "回答列表不能为空")
        List<AnswerRequest> answers
) {
}
