package com.chenhaonee.agents.app.application.scheduler;

import com.chenhaonee.agents.app.application.task.TaskEngine;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service("agentTaskScheduler")
@RequiredArgsConstructor
public class TaskScheduler {

    private final TaskRepository taskRepository;
    private final TaskEngine taskEngine;

    @Value("${agents.scheduler.task.enabled:true}")
    private boolean enabled;

    @Value("${agents.scheduler.task.start-time:00:00}")
    private LocalTime startTime;

    @Value("${agents.scheduler.task.end-time:23:59}")
    private LocalTime endTime;

    @Scheduled(fixedRateString = "${agents.scheduler.task.fixed-rate:300000}")
    public void scheduleTasks() {
        if (!enabled) {
            return;
        }
        LocalTime now = LocalTime.now();
        if (now.isBefore(startTime) || now.isAfter(endTime)) {
            return;
        }
        List<Task> activeTasks = taskRepository.findByStatusInAndValidIsTrue(
                List.of(TaskStatus.CREATED, TaskStatus.RUNNING)
        );
        for (Task task : activeTasks) {
            try {
                taskEngine.processPattern(task);
            } catch (Exception e) {
                log.error("Failed to process task {}", task.getCode(), e);
            }
        }
    }
}
