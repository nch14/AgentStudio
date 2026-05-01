package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 本地文件详情响应 DTO（不依赖 DB 记录，直接反映文件系统状态）。
 */
public record LocalFileDetailDTO(
        @Schema(description = "Agent 编码") String agentCode,
        @Schema(description = "逻辑路径") String path,
        @Schema(description = "文件名") String name,
        @Schema(description = "文件大小（字节）") Long fileSize,
        @Schema(description = "最后修改时间") String lastModifiedTime
) {
}
