package com.chenhaonee.agents.connect.capability;

import java.util.List;

public interface TaskFetchApi {

    /**
     * 根据任务 code 查询任务详情。
     */
    TaskFetchApi.TaskInfo getTaskByCode(String taskCode);

    /**
     * 根据任务 code 查询该任务的所有 turn 记录。
     */
    List<TurnRecord> getTurnsByTaskCode(String taskCode);

    /**
     * 任务基本信息。
     */
    record TaskInfo(
            String code,
            String title,
            String content,
            String status,
            String currentTurnCode,
            Integer currentTurnNo,
            int progress,
            String resultSummary
    ) {
    }

    /**
     * Turn 执行记录。
     */
    record TurnRecord(
            String code,
            int turnNo,
            String status,
            String finalSummary,
            String finalDetail
    ) {
    }
}
