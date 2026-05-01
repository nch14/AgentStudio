package com.chenhaonee.agents.connect.spi.model.task;

import java.util.Objects;

/**
 * 单轮 task 执行结果。
 * <p>
 * outcome 表达 turn 自身的执行状态；progress 表达 turn 结束时所观察到的项目进度（0-100），
 * 为空表示 Agent 本轮未对项目进度做新的评估，由 TurnEngine 沿用 turn 当前基线。
 */
public record TaskTurnResult(
        TaskNextAction outcome,
        Integer progress,
        String resultSummary,
        String resultDetail
) {

    public TaskTurnResult {
        Objects.requireNonNull(outcome, "outcome cannot be null");
        if (progress != null && (progress < 0 || progress > 100)) {
            throw new IllegalArgumentException("progress must be between 0 and 100, but was " + progress);
        }
        if (requiresSummary(outcome) && (resultSummary == null || resultSummary.isBlank())) {
            throw new IllegalArgumentException("resultSummary cannot be blank for outcome " + outcome);
        }
        if (outcome == TaskNextAction.WAIT_COORDINATION && (resultSummary != null || resultDetail != null)) {
            throw new IllegalArgumentException("WAIT_COORDINATION turn cannot carry result summary or detail");
        }
    }

    public static TaskTurnResult waitCoordination() {
        return new TaskTurnResult(TaskNextAction.WAIT_COORDINATION, null, null, null);
    }

    public static TaskTurnResult terminated(Integer progress, String resultSummary, String resultDetail) {
        return new TaskTurnResult(TaskNextAction.TERMINATED, progress, resultSummary, resultDetail);
    }

    public static TaskTurnResult hanging(String resultSummary, String resultDetail) {
        return new TaskTurnResult(TaskNextAction.HANGING, null, resultSummary, resultDetail);
    }

    private static boolean requiresSummary(TaskNextAction outcome) {
        return outcome == TaskNextAction.TERMINATED || outcome == TaskNextAction.HANGING;
    }
}
