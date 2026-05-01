package com.chenhaonee.agents.app.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.app.application.task.TaskEngine;
import com.chenhaonee.agents.app.application.task.TaskNotificationService;
import com.chenhaonee.agents.app.application.task.TaskTurnCoordinator;
import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class TaskEngineTest {

    private final TaskDomainService taskDomainService = mock(TaskDomainService.class);
    private final TaskTurnDomainService taskTurnDomainService = mock(TaskTurnDomainService.class);
    private final TaskTurnCoordinator taskTurnCoordinator = mock(TaskTurnCoordinator.class);
    private final AgentDomainService agentDomainService = mock(AgentDomainService.class);
    private final AgentRegistry agentRegistry = mock(AgentRegistry.class);
    private final TaskNotificationService taskNotificationService = mock(TaskNotificationService.class);
    private final TaskFactory taskFactory = new TaskFactory();

    @Test
    void shouldNotCreateNextTurnWhenLatestTurnIsHangingAndNotTimedOut() {
        TaskEngine taskEngine = new TaskEngine(taskDomainService, taskTurnDomainService, taskTurnCoordinator, agentDomainService, agentRegistry, taskNotificationService);
        ReflectionTestUtils.setField(taskEngine, "hangingTimeoutSeconds", 1800L);

        Task task = activeTask();
        TaskTurn latestTurn = hangingTurn(task.getCode(), Instant.now().minusSeconds(60));
        when(taskTurnDomainService.listTurns(task.getCode())).thenReturn(List.of(latestTurn));

        taskEngine.processPattern(task);

        verify(taskTurnCoordinator, never()).startNextTurn(eq(task.getCode()), anyInt());
        verify(taskTurnDomainService, never()).save(latestTurn);
    }

    @Test
    void shouldCancelTimedOutHangingTurnBeforeCreatingNextTurn() {
        TaskEngine taskEngine = new TaskEngine(taskDomainService, taskTurnDomainService, taskTurnCoordinator, agentDomainService, agentRegistry, taskNotificationService);
        ReflectionTestUtils.setField(taskEngine, "hangingTimeoutSeconds", 1800L);

        Task task = activeTask();
        TaskTurn latestTurn = hangingTurn(task.getCode(), Instant.now().minusSeconds(1801));
        when(taskTurnDomainService.listTurns(task.getCode())).thenReturn(List.of(latestTurn));

        taskEngine.processPattern(task);

        verify(taskTurnDomainService).save(latestTurn);
        verify(taskTurnCoordinator).startNextTurn(task.getCode(), task.getProgress());
        assertEquals(TurnRunStatus.CANCELLED, latestTurn.getRunStatus());
        assertFalse(latestTurn.isFinished());
    }

    @Test
    void shouldNotifySucceededTaskWithLatestTurnSummary() {
        TaskEngine taskEngine = new TaskEngine(taskDomainService, taskTurnDomainService, taskTurnCoordinator, agentDomainService, agentRegistry, taskNotificationService);

        Task task = activeTask();
        task.updateProgress(100);

        Task completedTask = activeTask();
        completedTask.setCode(task.getCode());
        completedTask.succeed("任务完成摘要");

        TaskTurn latestTurn = terminatedTurn(task.getCode(), "任务完成摘要");
        when(taskTurnDomainService.listTurns(task.getCode())).thenReturn(List.of(latestTurn));
        when(taskDomainService.complete(task.getCode(), "任务完成摘要")).thenReturn(completedTask);

        taskEngine.processPattern(task);

        verify(taskDomainService).complete(task.getCode(), "任务完成摘要");
        verify(taskNotificationService).notifyTaskSucceeded(completedTask, "任务完成摘要");
    }

    private Task activeTask() {
        Task task = taskFactory.createUserTask("整理任务", "assistant", "输出结果", "test-owner");
        task.startTurn("turn-1");
        task.markRunning();
        return task;
    }

    private TaskTurn hangingTurn(String taskCode, Instant finishedAt) {
        TaskTurn turn = new TaskTurn();
        turn.setCode("turn-1");
        turn.setTaskCode(taskCode);
        turn.setTurnNo(1);
        turn.setRunStatus(TurnRunStatus.HANGING);
        turn.setStartedAt(finishedAt.minusSeconds(10));
        turn.setFinishedAt(finishedAt);
        turn.setFinalSummary("Hanging");
        turn.setFinalDetail("Still hanging");
        turn.setProgress(0);
        return turn;
    }

    private TaskTurn terminatedTurn(String taskCode, String summary) {
        TaskTurn turn = new TaskTurn();
        turn.setCode("turn-1");
        turn.setTaskCode(taskCode);
        turn.setTurnNo(1);
        turn.setRunStatus(TurnRunStatus.TERMINATED);
        turn.setStartedAt(Instant.now().minusSeconds(10));
        turn.setFinishedAt(Instant.now());
        turn.setFinalSummary(summary);
        turn.setFinalDetail("done");
        turn.setProgress(100);
        return turn;
    }
}
