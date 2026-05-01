package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agent 文件条目信息")
public record AgentFileEntryDTO(
        @Schema(description = "条目类型") AgentFileEntryType type,
        @Schema(description = "条目名称") String name,
        @Schema(description = "逻辑路径") String path,
        @Schema(description = "文件编码 (仅当 type 为 FILE 时有效)") String fileCode,
        @Schema(description = "文件大小 (仅当 type 为 FILE 时有效)") Long fileSize,
        @Schema(description = "更新时间") String updateTime
) {
    public static AgentFileEntryDTO directory(String name, String path) {
        return new AgentFileEntryDTO(AgentFileEntryType.DIRECTORY, name, path, null, null, null);
    }
}
