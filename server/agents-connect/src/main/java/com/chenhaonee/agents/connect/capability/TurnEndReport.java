package com.chenhaonee.agents.connect.capability;

/**
 * Agent 通过 reportTurnEnd 工具上报的 turn 终态信息。
 */
public record TurnEndReport(
        Integer progress,
        String summary,
        String detail
) {
}
