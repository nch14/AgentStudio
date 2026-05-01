package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * 云端文件详情响应 DTO（基于 CloudFile DB 备份记录）。
 */
public record CloudFileDetailDTO(
        @Schema(description = "文件编码") String code,
        @Schema(description = "关联的 Agent 编码") String agentCode,
        @Schema(description = "逻辑路径") String path,
        @Schema(description = "文件名") String name,
        @Schema(description = "文件大小（字节）") Long fileSize,
        @Schema(description = "OSS 存储 Key") String ossKey,
        @Schema(description = "最后同步时间") String lastSyncTime,
        @Schema(description = "创建时间") String createTime,
        @Schema(description = "更新时间") String updateTime
) {
}
