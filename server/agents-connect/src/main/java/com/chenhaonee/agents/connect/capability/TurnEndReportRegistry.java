package com.chenhaonee.agents.connect.capability;

/**
 * 存储 Agent 通过 reportTurnEnd 工具上报的 turn 终态，按 turnCode 索引。
 * 每个 turnCode 只保留最新一份上报（后写覆盖前写）。
 */
public interface TurnEndReportRegistry {

    /**
     * 写入或覆盖 turnCode 对应的上报内容。
     */
    void report(String turnCode, TurnEndReport report);

    /**
     * 读取并移除 turnCode 对应的上报内容。若不存在则返回 null。
     */
    TurnEndReport consume(String turnCode);
}
