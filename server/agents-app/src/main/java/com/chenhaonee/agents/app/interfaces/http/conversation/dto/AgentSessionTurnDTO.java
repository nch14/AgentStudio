package com.chenhaonee.agents.app.interfaces.http.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

/**
 * Agent 会话 turn 响应 DTO。一个 turn 内包含按 messageIndex 升序的多个 block。
 */
public record AgentSessionTurnDTO(
        @Schema(description = "turn 编码") String turnCode,
        @Schema(description = "turn 内的 block 列表") List<AgentSessionBlockDTO> blocks
) {
}
