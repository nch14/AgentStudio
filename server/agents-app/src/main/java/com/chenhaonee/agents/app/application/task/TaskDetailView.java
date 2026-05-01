package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;

/**
 * task 详情页聚合视图。
 */
public record TaskDetailView(
        Task task,
        TaskTurn currentTurn
) {
}
