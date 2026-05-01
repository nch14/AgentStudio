package com.chenhaonee.agents.app.interfaces.http.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 取消任务请求。
 */
public record TaskCancelRequest(
        @Schema(description = "取消原因", example = "先停下来，我要改需求") String reason
) {
}
