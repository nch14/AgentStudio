package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 创建本地文件请求。
 */
public record LocalFileCreateRequest(
        @Schema(description = "逻辑路径，如 home/memory/daily/", requiredMode = Schema.RequiredMode.REQUIRED) String path,
        @Schema(description = "文件名，如 note.md", requiredMode = Schema.RequiredMode.REQUIRED) String name,
        @Schema(description = "文件内容") String content
) {
}
