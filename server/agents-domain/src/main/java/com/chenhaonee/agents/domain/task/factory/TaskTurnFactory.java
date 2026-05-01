package com.chenhaonee.agents.domain.task.factory;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import java.time.Instant;
import org.springframework.stereotype.Component;

/**
 * 任务回合工厂。
 */
@Component
public class TaskTurnFactory {

    /**
     * @param baselineProgress 项目维度的当前进度（0-100），作为新 turn 的初始基线，
     *                         若 Agent 在本轮未对进度做新评估则沿用该值。
     */
    public TaskTurn create(String taskCode, int turnNo, int baselineProgress) {
        if (baselineProgress < 0 || baselineProgress > 100) {
            throw new IllegalArgumentException("baselineProgress must be between 0 and 100, but was " + baselineProgress);
        }
        TaskTurn taskTurn = new TaskTurn();
        taskTurn.setTaskCode(ValidationUtils.requireText(taskCode, "taskCode"));
        taskTurn.setTurnNo(turnNo);
        taskTurn.setRunStatus(TurnRunStatus.RUNNING);
        taskTurn.setStartedAt(Instant.now());
        taskTurn.setProgress(baselineProgress);
        return taskTurn;
    }
}
