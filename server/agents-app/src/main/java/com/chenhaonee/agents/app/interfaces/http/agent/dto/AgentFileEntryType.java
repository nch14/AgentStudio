package com.chenhaonee.agents.app.interfaces.http.agent.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Agent 文件条目类型")
public enum AgentFileEntryType {
    FILE,
    DIRECTORY
}
