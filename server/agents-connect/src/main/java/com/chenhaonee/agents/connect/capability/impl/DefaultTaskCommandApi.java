package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.TaskCommandApi;
import com.chenhaonee.agents.domain.coordination.repository.QuestionsRepository;
import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import com.chenhaonee.agents.domain.task.repository.TaskTurnRepository;
import org.springframework.stereotype.Component;

/**
 * TaskCommandApi 的 connect 模块实现。
 * <p>
 * 通过 domain 层的 TaskFactory 和 TaskRepository 实现任务管理。
 */
@Component
public class DefaultTaskCommandApi implements TaskCommandApi {

    private final TaskRepository taskRepository;
    private final TaskTurnRepository taskTurnRepository;
    private final QuestionsRepository questionsRepository;
    private final TaskFactory taskFactory;

    public DefaultTaskCommandApi(TaskRepository taskRepository,
                                 TaskTurnRepository taskTurnRepository,
                                 QuestionsRepository questionsRepository,
                                 TaskFactory taskFactory) {
        this.taskRepository = taskRepository;
        this.taskTurnRepository = taskTurnRepository;
        this.questionsRepository = questionsRepository;
        this.taskFactory = taskFactory;
    }

    @Override
    public String createTask(String title, String content, String agentCode) {
        Task task = taskFactory.createUserTask(title, agentCode, content, null);
        return taskRepository.save(task).getCode();
    }

    @Override
    public void retryTask(String taskCode) {
        Task task = requireTask(taskCode);
        task.setStatus(TaskStatus.CREATED);
        task.setCurrentTurnCode(null);
        task.setFinishedAt(null);
        task.setResultSummary(null);
        taskRepository.save(task);
    }

    @Override
    public void cancelTask(String taskCode, String reason) {
        Task task = requireTask(taskCode);
        task.cancel(reason);
        taskRepository.save(task);
    }

    @Override
    public TaskDetail getTaskDetail(String taskCode) {
        Task task = requireTask(taskCode);
        Integer currentTurnNo = task.getCurrentTurnCode() == null
                ? null
                : taskTurnRepository.findByCode(task.getCurrentTurnCode())
                        .map(taskTurn -> taskTurn.getTurnNo())
                        .orElse(null);
        String activeQuestionsCode = questionsRepository
                .findFirstByTaskCodeAndResolvedFalseOrderByOpenedAtDesc(taskCode)
                .map(questions -> questions.getCode())
                .orElse(null);
        return new TaskDetail(
                task.getCode(),
                task.getTitle(),
                task.getContent(),
                task.getStatus().name(),
                task.getCurrentTurnCode(),
                currentTurnNo,
                task.getProgress(),
                activeQuestionsCode,
                task.getResultSummary());
    }

    private Task requireTask(String taskCode) {
        return taskRepository.findByCode(taskCode)
                .orElseThrow(() -> new IllegalArgumentException("task not found: " + taskCode));
    }
}
