package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.coordination.model.Answer;
import com.chenhaonee.agents.domain.coordination.service.QuestionsDomainService;
import com.chenhaonee.agents.domain.profile.service.OwnerProfileDomainService;
import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * task 命令应用服务。
 */
@Service
@RequiredArgsConstructor
public class TaskCommandApplicationService {

    private final TaskDomainService taskDomainService;
    private final TaskTurnDomainService taskTurnDomainService;
    private final QuestionsDomainService questionsDomainService;
    private final OwnerProfileDomainService ownerProfileDomainService;
    private final TaskFactory taskFactory;
    private final TaskRepository taskRepository;
    private final TransactionTemplate transactionTemplate;

    public Task resolveQuestions(String taskCode, String questionsCode, List<Answer> answers) {
        transactionTemplate.executeWithoutResult(status -> {
            questionsDomainService.resolveQuestions(taskCode, questionsCode, answers);
        });
        return taskDomainService.requireTask(taskCode);
    }

    public Task cancel(String taskCode, String reason) {
        return transactionTemplate.execute(status -> {
            Task task = taskDomainService.requireTask(taskCode);
            String cancelReason = StringUtils.defaultIfBlank(reason, "Boss 取消了任务");
            if (task.getCurrentTurnCode() != null) {
                TaskTurn taskTurn = requireCurrentTurn(task);
                if (taskTurn.getFinishedAt() == null) {
                    taskTurn.cancel(cancelReason, cancelReason);
                    taskTurnDomainService.save(taskTurn);
                }
            }
            task.cancel(cancelReason);
            return task;
        });
    }

    public void resumeTurn(String turnCode) {
        TaskTurn taskTurn = taskTurnDomainService.requireTurn(turnCode);
        if (taskTurn.getRunStatus() != TurnRunStatus.HANGING) {
            throw new IllegalStateException("task turn can only be resumed from HANGING status: " + turnCode);
        }
        transactionTemplate.executeWithoutResult(status -> {
            taskTurn.resumeFromHanging();
            taskTurnDomainService.save(taskTurn);
        });
    }

    public void retry(String taskCode) {
        Task task = taskDomainService.requireTask(taskCode);
        if (task.getStatus() == TaskStatus.CREATED
                || task.getStatus() == TaskStatus.RUNNING) {
            throw new IllegalStateException("task is already active: " + taskCode);
        }
        transactionTemplate.executeWithoutResult(status -> {
            Task managedTask = taskDomainService.requireTask(taskCode);
            managedTask.retry();
            taskRepository.save(managedTask);
            TaskTurn taskTurn = taskTurnDomainService.createNextTurn(taskCode, managedTask.getProgress());
            taskDomainService.startTurn(taskCode, taskTurn.getCode());
        });
    }

    public Task rollback(String taskCode) {
        Task task = taskDomainService.requireTask(taskCode);
        if (!task.isTerminal()) {
            throw new IllegalStateException("task cannot be rolled back from status " + task.getStatus());
        }
        transactionTemplate.executeWithoutResult(status -> {
            task.rollback();
            taskRepository.save(task);
        });
        return task;
    }

    @Transactional(rollbackFor = Exception.class)
    public Task createTask(String title, String agentCode, String description) {
        String ownerCode = ownerProfileDomainService.requireCurrent().getCode();
        Task task = taskFactory.createUserTask(title, agentCode, description, ownerCode);
        return taskRepository.save(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public Task updateTask(String taskCode, String title, String description) {
        Task task = taskDomainService.requireTask(taskCode);
        if (title != null && !title.isBlank()) {
            task.setTitle(title);
        }
        if (description != null && !description.isBlank()) {
            task.setContent(description);
        }
        return taskRepository.save(task);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteTask(String taskCode) {
        Task task = taskDomainService.requireTask(taskCode);
        // Cancel current turn if running
        if (task.getCurrentTurnCode() != null) {
            try {
                TaskTurn taskTurn = taskTurnDomainService.requireTurn(task.getCurrentTurnCode());
                if (taskTurn.getFinishedAt() == null) {
                    taskTurn.cancel("Task deleted", "The parent task has been deleted, cancelling this turn.");
                    taskTurnDomainService.save(taskTurn);
                }
            } catch (java.util.NoSuchElementException e) {
                // Turn may have already been finished, ignore
            }
        }
        // Cancel the task itself if it's in a non-terminal active state
        if (task.getStatus() == TaskStatus.CREATED || task.getStatus() == TaskStatus.RUNNING) {
            task.cancel("The task has been deleted.");
        }
        task.setValid(false);
        taskRepository.save(task);
    }

    private TaskTurn requireCurrentTurn(Task task) {
        if (StringUtils.isBlank(task.getCurrentTurnCode())) {
            throw new IllegalStateException("task has no current turn: " + task.getCode());
        }
        return taskTurnDomainService.requireTurn(task.getCurrentTurnCode());
    }
}
