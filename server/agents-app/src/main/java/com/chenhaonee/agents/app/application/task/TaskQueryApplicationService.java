package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.coordination.model.Questions;
import com.chenhaonee.agents.domain.coordination.repository.QuestionsRepository;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import lombok.RequiredArgsConstructor;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * task 查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class TaskQueryApplicationService {

    private final TaskDomainService taskDomainService;
    private final TaskTurnDomainService taskTurnDomainService;
    private final QuestionsRepository questionsRepository;

    public TaskDetailView getTaskDetail(String taskCode) {
        Task task = taskDomainService.requireTask(taskCode);
        TaskTurn currentTurn = resolveCurrentTurn(task);
        return new TaskDetailView(task, currentTurn);
    }

    public Page<Task> listTasks(int page, int size, TaskSource source, TaskStatus status, String sourceRef) {
        return taskDomainService.listTasks(source, status, sourceRef, PageRequest.of(page, size));
    }

    public List<TaskTurn> listTurns(String taskCode) {
        return taskTurnDomainService.listTurns(taskCode);
    }

    public Optional<Questions> getActiveQuestions(String taskCode) {
        taskDomainService.requireTask(taskCode);
        return questionsRepository.findFirstByTaskCodeAndResolvedFalseOrderByOpenedAtDesc(taskCode);
    }

    public TaskTurn getTurn(String turnCode) {
        return taskTurnDomainService.requireTurn(turnCode);
    }

    private TaskTurn resolveCurrentTurn(Task task) {
        if (task.getCurrentTurnCode() == null) {
            return null;
        }
        try {
            return taskTurnDomainService.requireTurn(task.getCurrentTurnCode());
        } catch (NoSuchElementException ignored) {
            return null;
        }
    }
}
