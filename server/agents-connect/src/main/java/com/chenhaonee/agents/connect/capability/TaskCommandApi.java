package com.chenhaonee.agents.connect.capability;

/**
 * Agent 可调用的任务管理能力。
 */
public interface TaskCommandApi {

    /**
     * 创建新任务。
     */
    String createTask(String title, String content, String agentCode);

    /**
     * 重试指定任务。
     */
    void retryTask(String taskCode);

    /**
     * 取消指定任务。
     */
    void cancelTask(String taskCode, String reason);

    /**
     * 获取任务详情。
     */
    TaskDetail getTaskDetail(String taskCode);

    /**
     * 任务详情简化 DTO。
     */
    record TaskDetail(
            String taskCode,
            String title,
            String content,
            String status,
            String currentTurnCode,
            Integer currentTurnNo,
            int progress,
            String activeQuestionsCode,
            String resultSummary
    ) {
    }
}
