package com.chenhaonee.agents.app.application.scheduler;

import com.chenhaonee.agents.app.application.task.TurnEngine;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import com.chenhaonee.agents.domain.task.model.TurnRunStatus;
import com.chenhaonee.agents.domain.task.repository.TaskTurnRepository;
import com.chenhaonee.agents.domain.task.service.TaskDomainService;
import com.chenhaonee.agents.domain.task.service.TaskTurnDomainService;
import java.util.NoSuchElementException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class TurnScheduler {

    private final TaskTurnRepository taskTurnRepository;
    private final TaskDomainService taskDomainService;
    private final TurnEngine turnEngine;
    private final TaskTurnDomainService taskTurnDomainService;

    @Value("${agents.scheduler.turn.enabled:true}")
    private boolean enabled;

    @Value("${agents.scheduler.turn.start-time:00:00}")
    private LocalTime startTime;

    @Value("${agents.scheduler.turn.end-time:23:59}")
    private LocalTime endTime;

    // In-memory set to prevent picking up the same turn concurrently across scheduler ticks
    private final Set<String> executingTurns = ConcurrentHashMap.newKeySet();

    @Scheduled(fixedRateString = "${agents.scheduler.turn.fixed-rate:300000}")
    public void scheduleTurns() {
        if (!enabled) {
            return;
        }
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return;
        }
        List<TaskTurn> runningTurns = taskTurnRepository.findByRunStatus(TurnRunStatus.RUNNING);
        for (TaskTurn turn : runningTurns) {
            // Only execute if not already being processed
            if (executingTurns.add(turn.getCode())) {
                try {
                    var task = taskDomainService.requireTask(turn.getTaskCode());
                    turnEngine.runTurn(task, turn);
                } catch (NoSuchElementException e) {
                    log.warn("Task {} has been deleted, cancelling turn {}", turn.getTaskCode(), turn.getCode());
                    turn.cancel("Task deleted", "The parent task has been deleted, cancelling this turn.");
                    taskTurnDomainService.save(turn);
                } catch (Exception e) {
                    log.error("Failed to process turn {}", turn.getCode(), e);
                } finally {
                    executingTurns.remove(turn.getCode());
                }
            }
        }
    }
}
