package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.connect.spi.model.task.TaskPrepareResult;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TaskEngine {

    private final TaskDomainService taskDomainService;
    private final TaskTurnDomainService taskTurnDomainService;
    private final TaskTurnCoordinator taskTurnCoordinator;
    private final AgentDomainService agentDomainService;
    private final AgentRegistry agentRegistry;
    private final TaskNotificationService taskNotificationService;

    @Value("${agents.scheduler.task.hanging-timeout-seconds:1800}")
    private long hangingTimeoutSeconds = 1800;

    public void processPattern(Task task) {
        List<TaskTurn> turns = taskTurnDomainService.listTurns(task.getCode());
        TaskTurn latestTurn = turns.isEmpty() ? null : turns.get(0);

        if (latestTurn == null) {
            if (task.getStatus() == TaskStatus.CREATED) {
                initNewTask(task);
            }
            return;
        }

        if (latestTurn.getRunStatus() == TurnRunStatus.HANGING) {
            processHangingTurn(task, latestTurn);
            return;
        }

        // Evaluate task progress based on latest turn
        if (isTurnEnded(latestTurn)) {
            if (task.getProgress() == 100) {
                Task completedTask = taskDomainService.complete(task.getCode(), latestTurn.getFinalSummary());
                taskNotificationService.notifyTaskSucceeded(completedTask, latestTurn.getFinalSummary());
            } else {
                startNextTurn(task);
            }
        }
    }

    private void processHangingTurn(Task task, TaskTurn latestTurn) {
        if (!isHangingTimedOut(latestTurn)) {
            log.info("Task {} latest turn {} is HANGING but not timed out yet, skip creating next turn",
                    task.getCode(), latestTurn.getCode());
            return;
        }

        latestTurn.timeoutCancel("HANGING turn timeout",
                "Turn remained HANGING beyond timeout, cancelled by scheduler before starting next turn.");
        taskTurnDomainService.save(latestTurn);
        startNextTurn(task);
    }

    private boolean isHangingTimedOut(TaskTurn turn) {
        Instant hangingSince = turn.getFinishedAt() != null ? turn.getFinishedAt() : turn.getStartedAt();
        return hangingSince != null && !hangingSince.plusSeconds(hangingTimeoutSeconds).isAfter(Instant.now());
    }

    private boolean isTurnEnded(TaskTurn turn) {
        return turn.getRunStatus() == TurnRunStatus.TERMINATED
                || turn.getRunStatus() == TurnRunStatus.HANGING
                || turn.getRunStatus() == TurnRunStatus.CANCELLED;
    }

    private void initNewTask(Task task) {
        try {
            Agent agent = agentDomainService.requireEnabledAgent(task.getAgentCode());
            TaskAgent taskAgent = agentRegistry.findTaskAgent(agent.getProvider())
                    .orElseThrow(() -> new IllegalStateException("Task agent not found"));

            TaskPrepareResult prepareResult = taskAgent.prepare(agent.getCode(), task.getCode());
            if (!prepareResult.ready()) {
                log.error("Task {} failed to prepare: {}", task.getCode(), prepareResult.failureReason());
                return;
            }

            startNextTurn(task);
        } catch (Exception e) {
            log.error("Failed to init task {}", task.getCode(), e);
        }
    }

    private void startNextTurn(Task task) {
        taskTurnCoordinator.startNextTurn(task.getCode(), task.getProgress());
    }
}
