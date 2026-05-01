package com.chenhaonee.agents.app.scheduler;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.app.application.scheduler.PlanScheduler;
import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import com.chenhaonee.agents.domain.schedule.service.AutomationPlanDomainService;
import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class PlanSchedulerTest {

    private final AutomationPlanDomainService planDomainService = org.mockito.Mockito.mock(AutomationPlanDomainService.class);
    private final TaskFactory taskFactory = new TaskFactory();
    private final TaskRepository taskRepository = org.mockito.Mockito.mock(TaskRepository.class);

    private PlanScheduler planScheduler;

    @BeforeEach
    void setUp() {
        planScheduler = new PlanScheduler(planDomainService, taskFactory, taskRepository);
    }

    @Test
    void shouldNotScanWhenDisabled() {
        // enabled 默认 false（Java boolean 原始类型默认值），无需反射设置
        planScheduler.scanPlans();

        verify(planDomainService, never()).findExecutablePlans();
    }

    @Test
    void shouldSkipWhenNoPlans() {
        ReflectionTestUtils.setField(planScheduler, "enabled", true);
        when(planDomainService.findExecutablePlans()).thenReturn(Collections.emptyList());

        planScheduler.scanPlans();

        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldNotTriggerPlanWithFutureCron() {
        ReflectionTestUtils.setField(planScheduler, "enabled", true);
        // Cron "0 8 * * *" fires at 8am daily. createTime is 1 hour ago, so next scheduled is 8am today or tomorrow
        Instant createTime = Instant.now().minusSeconds(3600);
        AutomationPlan plan = createPlan("plan-001", "晨报", "agent-001", "0 8 * * *", createTime);

        when(planDomainService.findExecutablePlans()).thenReturn(List.of(plan));

        planScheduler.scanPlans();

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldTriggerPlanWhenCronMatches() {
        ReflectionTestUtils.setField(planScheduler, "enabled", true);
        // Cron "0/30 * * * * *" fires every 30 seconds. createTime is 2 minutes ago, so there should be matches
        Instant createTime = Instant.now().minusSeconds(120);
        AutomationPlan plan = createPlan("plan-001", "晨报", "agent-001", "0/30 * * * * *", createTime);

        when(planDomainService.findExecutablePlans()).thenReturn(List.of(plan));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        planScheduler.scanPlans();

        verify(taskRepository).save(any(Task.class));
        verify(planDomainService).save(plan);
        assertNotNull(plan.getLastTriggeredAt());
    }

    @Test
    void shouldNotReTriggerPlanAlreadyFired() {
        ReflectionTestUtils.setField(planScheduler, "enabled", true);
        AutomationPlan plan = createPlan("plan-001", "晨报", "agent-001", "0 8 * * *", Instant.now().minusSeconds(86400));
        plan.setLastTriggeredAt(Instant.now());

        when(planDomainService.findExecutablePlans()).thenReturn(List.of(plan));

        planScheduler.scanPlans();

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldHandleInvalidCronGracefully() {
        ReflectionTestUtils.setField(planScheduler, "enabled", true);
        AutomationPlan plan = createPlan("plan-001", "晨报", "agent-001", "invalid-cron", Instant.now().minusSeconds(120));

        when(planDomainService.findExecutablePlans()).thenReturn(List.of(plan));

        planScheduler.scanPlans();

        verify(taskRepository, never()).save(any(Task.class));
    }

    @Test
    void shouldCreateTaskWithCorrectAttributes() {
        ReflectionTestUtils.setField(planScheduler, "enabled", true);
        Instant createTime = Instant.now().minusSeconds(120);
        AutomationPlan plan = createPlan("plan-001", "晨报", "agent-001", "0/30 * * * * *", createTime);

        when(planDomainService.findExecutablePlans()).thenReturn(List.of(plan));
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        planScheduler.scanPlans();

        verify(taskRepository).save(argThat(task ->
                "晨报".equals(task.getTitle())
                        && "agent-001".equals(task.getAgentCode())
                        && task.getSource() == TaskSource.SCHEDULED_CREATE
                        && "plan-001".equals(task.getSourceRef())));
    }

    private AutomationPlan createPlan(String code, String name, String agentCode, String scheduleRule, Instant createTime) {
        AutomationPlan plan = new AutomationPlan();
        plan.setCode(code);
        plan.setName(name);
        plan.setAgentCode(agentCode);
        plan.setInstruction("执行自动化任务");
        plan.setScheduleRule(scheduleRule);
        plan.setDeliveryMode(DeliveryMode.PUSH);
        plan.setStatus(Status.ENABLED);
        plan.setCreateTime(Date.from(createTime));
        plan.setUpdateTime(new Date());
        return plan;
    }
}
