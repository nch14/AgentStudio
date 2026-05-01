package com.chenhaonee.agents.domain.task.service;

import com.chenhaonee.agents.domain.task.factory.TaskTurnFactory;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.repository.TaskTurnRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务回合领域服务。
 */
@Service
public class TaskTurnDomainService {

    private final TaskTurnRepository taskTurnRepository;
    private final TaskTurnFactory taskTurnFactory;

    public TaskTurnDomainService(TaskTurnRepository taskTurnRepository, TaskTurnFactory taskTurnFactory) {
        this.taskTurnRepository = taskTurnRepository;
        this.taskTurnFactory = taskTurnFactory;
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskTurn createNextTurn(String taskCode, int baselineProgress) {
        int nextTurnNo = taskTurnRepository.findTopByTaskCodeOrderByTurnNoDesc(taskCode)
                .map(taskTurn -> taskTurn.getTurnNo() + 1)
                .orElse(1);
        TaskTurn taskTurn = taskTurnFactory.create(taskCode, nextTurnNo, baselineProgress);
        return taskTurnRepository.save(taskTurn);
    }

    public TaskTurn requireTurn(String turnCode) {
        return taskTurnRepository.findByCode(turnCode)
                .orElseThrow(() -> new NoSuchElementException("task turn not found: " + turnCode));
    }

    public TaskTurn requireLatestTurn(String taskCode) {
        return taskTurnRepository.findTopByTaskCodeOrderByTurnNoDesc(taskCode)
                .orElseThrow(() -> new NoSuchElementException("task turn not found for task: " + taskCode));
    }

    public List<TaskTurn> listTurns(String taskCode) {
        return taskTurnRepository.findByTaskCodeOrderByTurnNoDesc(taskCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public TaskTurn save(TaskTurn taskTurn) {
        return taskTurnRepository.save(taskTurn);
    }
}
