package com.chenhaonee.agents.connect.capability;

/**
 * Agent 可调用的自动化任务管理能力。
 */
public interface AutomationApi {

    /**
     * 创建自动化任务模板。
     */
    String createAutomationPlan(String name, String agentCode, String instruction, String scheduleRule, String deliveryMode);

    /**
     * 更新自动化任务模板。
     */
    void updateAutomationPlan(String planCode, String name, String instruction, String scheduleRule, String deliveryMode);
}
