package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 覆盖写入本地文件内容请求。
 */
public record LocalFileWriteContentRequest(
        @Schema(description = "逻辑路径（含文件名），如 home/memory/daily/note.md", requiredMode = Schema.RequiredMode.REQUIRED) String path,
        @Schema(description = "文件内容", requiredMode = Schema.RequiredMode.REQUIRED) String content
) {
}
