package com.chenhaonee.agents.app.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.app.application.task.TaskNotificationService;
import com.chenhaonee.agents.app.application.task.TurnEngine;
import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnRequest;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnResult;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import com.chenhaonee.agents.common.domain.Status;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TurnEngineTest {

    private final AgentRegistry agentRegistry = mock(AgentRegistry.class);
    private final AgentDomainService agentDomainService = mock(AgentDomainService.class);
    private final TaskTurnDomainService taskTurnDomainService = mock(TaskTurnDomainService.class);
    private final TaskDomainService taskDomainService = mock(TaskDomainService.class);
    private final TaskNotificationService taskNotificationService = mock(TaskNotificationService.class);
    private final TaskAgent taskAgent = mock(TaskAgent.class);

    @Test
    void shouldSyncReportedProgressToTaskWhenTurnTerminates() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 20);
        TaskTurn turn = runningTurnAt("task-1", 20);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(taskAgent));
        when(taskAgent.runTurn(any(), any(TaskTurnRequest.class)))
                .thenReturn(TaskTurnResult.terminated(60, "进度更新", "detail"));

        engine.runTurn(task, turn);

        assertEquals(60, turn.getProgress());
        assertEquals(TurnRunStatus.TERMINATED, turn.getRunStatus());
        verify(taskDomainService).updateProgress("task-1", 60);
    }

    @Test
    void shouldNotResetTaskProgressWhenAgentReturnsHanging() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 40);
        TaskTurn turn = runningTurnAt("task-1", 40);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(taskAgent));
        when(taskAgent.runTurn(any(), any(TaskTurnRequest.class)))
                .thenReturn(TaskTurnResult.hanging("挂起", "detail"));

        engine.runTurn(task, turn);

        // 挂起不动 progress：turn 沿用基线 40，task 也没变 → 不写库
        assertEquals(40, turn.getProgress());
        assertEquals(TurnRunStatus.HANGING, turn.getRunStatus());
        verify(taskDomainService, never()).updateProgress(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(taskNotificationService).notifyTaskHanging(task, turn);
        verify(taskNotificationService, never()).notifyTaskWaiting(task);
    }

    @Test
    void shouldSuspendTurnAndNotifyWaitingWhenAgentNeedsCoordination() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 40);
        TaskTurn turn = runningTurnAt("task-1", 40);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(taskAgent));
        when(taskAgent.runTurn(any(), any(TaskTurnRequest.class)))
                .thenReturn(TaskTurnResult.waitCoordination());

        engine.runTurn(task, turn);

        assertEquals(40, turn.getProgress());
        assertEquals(TurnRunStatus.SUSPENDED, turn.getRunStatus());
        verify(taskDomainService, never()).updateProgress(any(), org.mockito.ArgumentMatchers.anyInt());
        verify(taskNotificationService).notifyTaskWaiting(task);
        verify(taskNotificationService, never()).notifyTaskHanging(task, turn);
    }

    @Test
    void shouldNotifyHangingWhenAgentCannotBeLoaded() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 40);
        TaskTurn turn = runningTurnAt("task-1", 40);

        when(agentDomainService.requireEnabledAgent("agent-1"))
                .thenThrow(new IllegalStateException("agent disabled"));

        engine.runTurn(task, turn);

        assertEquals(TurnRunStatus.HANGING, turn.getRunStatus());
        assertEquals("Agent Error", turn.getFinalSummary());
        verify(taskTurnDomainService).save(turn);
        verify(taskNotificationService).notifyTaskHanging(task, turn);
        verify(taskNotificationService, never()).notifyTaskWaiting(task);
    }

    @Test
    void shouldNotifyHangingWhenTaskAgentIsMissing() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 40);
        TaskTurn turn = runningTurnAt("task-1", 40);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.empty());

        engine.runTurn(task, turn);

        assertEquals(TurnRunStatus.HANGING, turn.getRunStatus());
        assertEquals("Engine Error", turn.getFinalSummary());
        verify(taskTurnDomainService).save(turn);
        verify(taskNotificationService).notifyTaskHanging(task, turn);
        verify(taskNotificationService, never()).notifyTaskWaiting(task);
    }

    @Test
    void shouldNotifyHangingWhenAgentExecutionThrows() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 40);
        TaskTurn turn = runningTurnAt("task-1", 40);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(taskAgent));
        when(taskAgent.runTurn(any(), any(TaskTurnRequest.class)))
                .thenThrow(new IllegalStateException("run failed"));

        engine.runTurn(task, turn);

        assertEquals(TurnRunStatus.HANGING, turn.getRunStatus());
        assertEquals("Engine Error", turn.getFinalSummary());
        verify(taskTurnDomainService).save(turn);
        verify(taskNotificationService).notifyTaskHanging(task, turn);
        verify(taskNotificationService, never()).notifyTaskWaiting(task);
    }

    @Test
    void shouldKeepBaselineWhenAgentReportsTerminatedWithoutAssessment() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 30);
        TaskTurn turn = runningTurnAt("task-1", 30);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(taskAgent));
        when(taskAgent.runTurn(any(), any(TaskTurnRequest.class)))
                .thenReturn(TaskTurnResult.terminated(null, "本轮无评估", "detail"));

        engine.runTurn(task, turn);

        assertEquals(30, turn.getProgress());
        assertEquals(TurnRunStatus.TERMINATED, turn.getRunStatus());
        verify(taskDomainService, never()).updateProgress(any(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void shouldMarkTurnSucceededAndSyncProgress100WhenAgentReportsDone() {
        TurnEngine engine = newEngine();
        Task task = taskWithProgress("task-1", "agent-1", 60);
        TaskTurn turn = runningTurnAt("task-1", 60);

        when(agentDomainService.requireEnabledAgent("agent-1")).thenReturn(enabledAgent("agent-1"));
        when(agentRegistry.findTaskAgent(AgentProvider.CLAUDE_CODE)).thenReturn(Optional.of(taskAgent));
        when(taskAgent.runTurn(any(), any(TaskTurnRequest.class)))
                .thenReturn(TaskTurnResult.terminated(100, "任务完成", "detail"));

        engine.runTurn(task, turn);

        assertEquals(100, turn.getProgress());
        assertEquals(TurnRunStatus.TERMINATED, turn.getRunStatus());
        verify(taskDomainService).updateProgress("task-1", 100);
    }

    private TurnEngine newEngine() {
        return new TurnEngine(
                agentRegistry,
                agentDomainService,
                taskTurnDomainService,
                taskDomainService,
                taskNotificationService);
    }

    private Task taskWithProgress(String code, String agentCode, int progress) {
        Task task = new Task();
        task.setCode(code);
        task.setAgentCode(agentCode);
        task.setProgress(progress);
        return task;
    }

    private TaskTurn runningTurnAt(String taskCode, int baselineProgress) {
        TaskTurn turn = new TaskTurn();
        turn.setCode("turn-1");
        turn.setTaskCode(taskCode);
        turn.setTurnNo(1);
        turn.setRunStatus(TurnRunStatus.RUNNING);
        turn.setStartedAt(Instant.now());
        turn.setProgress(baselineProgress);
        return turn;
    }

    private Agent enabledAgent(String code) {
        Agent agent = new Agent();
        agent.setCode(code);
        agent.setProvider(AgentProvider.CLAUDE_CODE);
        agent.setStatus(Status.ENABLED);
        return agent;
    }
}
