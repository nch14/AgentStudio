package com.chenhaonee.agents.app.application.scheduler;

import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.service.AutomationPlanDomainService;
import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.List;

/**
 * 定时扫描 AutomationPlan，按 cron 表达式自动创建 Task。
 */
@Slf4j
@Service
public class PlanScheduler {

    private static final ZoneId ZONE = ZoneId.systemDefault();

    private final AutomationPlanDomainService automationPlanDomainService;
    private final TaskFactory taskFactory;
    private final TaskRepository taskRepository;

    @Value("${agents.scheduler.plan.enabled:true}")
    private boolean enabled;

    public PlanScheduler(AutomationPlanDomainService automationPlanDomainService,
                         TaskFactory taskFactory,
                         TaskRepository taskRepository) {
        this.automationPlanDomainService = automationPlanDomainService;
        this.taskFactory = taskFactory;
        this.taskRepository = taskRepository;
    }

    @Scheduled(fixedRateString = "${agents.scheduler.plan.fixed-rate:60000}")
    public void scanPlans() {
        if (!enabled) {
            return;
        }
        List<AutomationPlan> plans = automationPlanDomainService.findExecutablePlans();
        if (plans.isEmpty()) {
            return;
        }

        ZonedDateTime now = ZonedDateTime.now(ZONE);
        for (AutomationPlan plan : plans) {
            try {
                evaluateAndTrigger(plan, now);
            } catch (Exception e) {
                log.error("Failed to evaluate automation plan {}", plan.getCode(), e);
            }
        }
    }

    private void evaluateAndTrigger(AutomationPlan plan, ZonedDateTime now) {
        CronExpression cron;
        try {
            cron = CronExpression.parse(plan.getScheduleRule());
        } catch (Exception e) {
            log.warn("Invalid cron expression for plan {}: {}", plan.getCode(), plan.getScheduleRule());
            return;
        }

        ZonedDateTime lastTriggered = getLastTriggeredAt(plan);
        ZonedDateTime nextScheduled = cron.next(lastTriggered);
        while (nextScheduled != null && !nextScheduled.isAfter(now)) {
            createTaskFromPlan(plan);
            lastTriggered = ZonedDateTime.now(ZONE);
            nextScheduled = cron.next(lastTriggered);
        }
        plan.setLastTriggeredAt(lastTriggered.toInstant());
        automationPlanDomainService.save(plan);
    }

    private ZonedDateTime getLastTriggeredAt(AutomationPlan plan) {
        if (plan.getLastTriggeredAt() != null) {
            return plan.getLastTriggeredAt().atZone(ZONE);
        }
        return toZonedDateTime(plan.getCreateTime());
    }

    private ZonedDateTime toZonedDateTime(Date date) {
        return date != null ? date.toInstant().atZone(ZONE) : ZonedDateTime.now(ZONE);
    }

    private void createTaskFromPlan(AutomationPlan plan) {
        Task task = taskFactory.createScheduledTask(plan.getName(), plan.getAgentCode(), plan.getInstruction(), plan.getCode());
        taskRepository.save(task);
        log.info("Auto-created task {} from automation plan {}", task.getCode(), plan.getCode());
    }
}
