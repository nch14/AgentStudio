package com.chenhaonee.agents.domain.schedule.service;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import com.chenhaonee.agents.domain.schedule.repository.AutomationPlanRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 自动化任务模板领域服务。
 */
@Service
public class AutomationPlanDomainService {

    private final AutomationPlanRepository automationPlanRepository;

    public AutomationPlanDomainService(AutomationPlanRepository automationPlanRepository) {
        this.automationPlanRepository = automationPlanRepository;
    }

    public List<AutomationPlan> findExecutablePlans() {
        return automationPlanRepository.findByStatus(Status.ENABLED);
    }

    public Page<AutomationPlan> listPlans(Pageable pageable) {
        return automationPlanRepository.findByStatus(Status.ENABLED, pageable);
    }

    public Page<AutomationPlan> listPlans(Status status, Pageable pageable) {
        return automationPlanRepository.findByStatus(status, pageable);
    }

    public AutomationPlan requirePlan(String planCode) {
        return automationPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("automation plan not found: " + planCode));
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationPlan createPlan(String name, String agentCode, String instruction,
                                     String scheduleRule, DeliveryMode deliveryMode) {
        AutomationPlan plan = new AutomationPlan();
        plan.setName(name);
        plan.setAgentCode(agentCode);
        plan.setInstruction(instruction);
        plan.setScheduleRule(scheduleRule);
        plan.setDeliveryMode(deliveryMode);
        plan.setStatus(Status.ENABLED);
        return automationPlanRepository.save(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationPlan updatePlan(String planCode, String name, String instruction,
                                     String scheduleRule, DeliveryMode deliveryMode) {
        AutomationPlan plan = requirePlan(planCode);
        if (name != null && !name.isBlank()) {
            plan.setName(name);
        }
        if (instruction != null && !instruction.isBlank()) {
            plan.setInstruction(instruction);
        }
        if (scheduleRule != null && !scheduleRule.isBlank()) {
            plan.setScheduleRule(scheduleRule);
        }
        if (deliveryMode != null) {
            plan.setDeliveryMode(deliveryMode);
        }
        return automationPlanRepository.save(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deletePlan(String planCode) {
        AutomationPlan plan = requirePlan(planCode);
        automationPlanRepository.delete(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationPlan save(AutomationPlan plan) {
        return automationPlanRepository.save(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationPlan enable(String planId) {
        AutomationPlan plan = requirePlan(planId);
        plan.enable();
        return automationPlanRepository.save(plan);
    }

    @Transactional(rollbackFor = Exception.class)
    public AutomationPlan disable(String planId) {
        AutomationPlan plan = requirePlan(planId);
        plan.disable();
        return automationPlanRepository.save(plan);
    }

    private AutomationPlan getPlan(String planId) {
        return automationPlanRepository.findByCode(planId)
                .orElseThrow(() -> new IllegalArgumentException("automation plan not found: " + planId));
    }
}
