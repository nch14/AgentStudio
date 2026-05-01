package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.TaskFetchApi;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import com.chenhaonee.agents.domain.task.repository.TaskTurnRepository;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * TaskFetchApi 的默认实现。
 */
@Component
public class DefaultTaskFetchApi implements TaskFetchApi {

    private final TaskRepository taskRepository;
    private final TaskTurnRepository taskTurnRepository;

    public DefaultTaskFetchApi(TaskRepository taskRepository,
                               TaskTurnRepository taskTurnRepository) {
        this.taskRepository = taskRepository;
        this.taskTurnRepository = taskTurnRepository;
    }

    @Override
    public TaskInfo getTaskByCode(String taskCode) {
        Task task = requireTask(taskCode);
        Integer currentTurnNo = task.getCurrentTurnCode() == null
                ? null
                : taskTurnRepository.findByCode(task.getCurrentTurnCode())
                        .map(TaskTurn::getTurnNo)
                        .orElse(null);
        return new TaskInfo(
                task.getCode(),
                task.getTitle(),
                task.getContent(),
                task.getStatus().name(),
                task.getCurrentTurnCode(),
                currentTurnNo,
                task.getProgress(),
                task.getResultSummary()
        );
    }

    @Override
    public List<TurnRecord> getTurnsByTaskCode(String taskCode) {
        requireTask(taskCode);
        List<TaskTurn> turns = taskTurnRepository.findByTaskCodeOrderByTurnNoDesc(taskCode);
        return turns.stream()
                .map(turn -> new TurnRecord(
                        turn.getCode(),
                        turn.getTurnNo(),
                        turn.getRunStatus().name(),
                        turn.getFinalSummary(),
                        turn.getFinalDetail()
                ))
                .toList();
    }

    private Task requireTask(String taskCode) {
        return taskRepository.findByCode(taskCode)
                .orElseThrow(() -> new IllegalArgumentException("task not found: " + taskCode));
    }
}
