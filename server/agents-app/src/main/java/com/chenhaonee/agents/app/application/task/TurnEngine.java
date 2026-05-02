package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.connect.driver.AgentRegistry;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnRequest;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnResult;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 任务回合执行引擎。
 *
 * <p>在 agentCode 维度持有互斥锁，防止同一 agent 同时执行两个 task turn（CLAUDE.md 等共享资源会竞态）。
 * chat 不持锁（chat 与 task / chat 与 chat 均允许并发）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TurnEngine {

    private final AgentRegistry agentRegistry;
    private final AgentDomainService agentDomainService;
    private final TaskTurnDomainService taskTurnDomainService;
    private final TaskDomainService taskDomainService;
    private final TaskNotificationService taskNotificationService;

    /** agentCode → 互斥锁，保证同一 agent 下 task turn 串行执行 */
    private final ConcurrentHashMap<String, ReentrantLock> agentLocks = new ConcurrentHashMap<>();

    public void runTurn(Task task, TaskTurn turn) {
        Agent agent;
        try {
            agent = agentDomainService.requireEnabledAgent(task.getAgentCode());
        } catch (Exception e) {
            log.error("Failed to load agent for turn {}", turn.getCode(), e);
            turn.fail("Agent Error", e.getMessage());
            taskTurnDomainService.save(turn);
            taskNotificationService.notifyTaskHanging(task, turn);
            return;
        }

        ReentrantLock lock = agentLocks.computeIfAbsent(agent.getCode(), k -> new ReentrantLock());
        if (!lock.tryLock()) {
            log.warn("Agent {} is busy, another task turn is running. Hanging turn {}.", agent.getCode(), turn.getCode());
            turn.hang("Agent Busy", "Another task turn is already running for this agent.");
            taskTurnDomainService.save(turn);
            taskNotificationService.notifyTaskHanging(task, turn);
            return;
        }

        try {
            executeRun(agent, task, turn);
        } finally {
            lock.unlock();
        }
    }

    private void executeRun(Agent agent, Task task, TaskTurn turn) {
        try {
            TaskAgent taskAgent = agentRegistry.findTaskAgent(agent.getProvider())
                    .orElseThrow(() -> new IllegalStateException("Task agent not found for provider: " + agent.getProvider()));
            TaskTurnRequest request = new TaskTurnRequest(task.getCode(), turn.getCode());
            TaskTurnResult result = taskAgent.runTurn(agent.getCode(), request);
            processResult(turn, task, result);
        } catch (Exception e) {
            log.error("Failed to run turn {}", turn.getCode(), e);
            turn.fail("Engine Error", e.getMessage());
            taskTurnDomainService.save(turn);
            taskNotificationService.notifyTaskHanging(task, turn);
        }
    }

    private void processResult(TaskTurn turn, Task task, TaskTurnResult result) {
        switch (result.outcome()) {
            case WAIT_COORDINATION -> turn.waitOwner();
            case HANGING -> turn.hang(result.resultSummary(), result.resultDetail());
            case TERMINATED -> {
                Integer reportedProgress = result.progress();
                if (reportedProgress != null && reportedProgress == 100) {
                    turn.succeed(result.resultSummary(), result.resultDetail());
                } else {
                    turn.completeTurn(result.resultSummary(), result.resultDetail(), reportedProgress);
                }
            }
        }
        taskTurnDomainService.save(turn);

        // turn 终结时把 progress 同步回写到项目维度，使 Task.progress 始终等于最近一次 turn 终结时的快照
        if (turn.getRunStatus() != TurnRunStatus.RUNNING && turn.getRunStatus() != TurnRunStatus.SUSPENDED) {
            int turnProgress = turn.getProgress();
            if (turnProgress != task.getProgress()) {
                taskDomainService.updateProgress(task.getCode(), turnProgress);
            }
        }

        if (turn.getRunStatus() == TurnRunStatus.SUSPENDED) {
            taskNotificationService.notifyTaskWaiting(task);
        } else if (turn.getRunStatus() == TurnRunStatus.HANGING) {
            taskNotificationService.notifyTaskHanging(task, turn);
        }
    }
}
