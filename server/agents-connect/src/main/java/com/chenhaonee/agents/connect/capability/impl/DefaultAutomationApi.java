package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.AutomationApi;
import com.chenhaonee.agents.domain.schedule.factory.AutomationPlanFactory;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import com.chenhaonee.agents.domain.schedule.repository.AutomationPlanRepository;
import org.springframework.stereotype.Component;

/**
 * AutomationApi 的 connect 模块实现。
 * <p>
 * 通过 domain 层的 AutomationPlanFactory 和 AutomationPlanRepository 实现自动化任务模板管理。
 */
@Component
public class DefaultAutomationApi implements AutomationApi {

    private final AutomationPlanRepository automationPlanRepository;
    private final AutomationPlanFactory automationPlanFactory;

    public DefaultAutomationApi(AutomationPlanRepository automationPlanRepository,
                                AutomationPlanFactory automationPlanFactory) {
        this.automationPlanRepository = automationPlanRepository;
        this.automationPlanFactory = automationPlanFactory;
    }

    @Override
    public String createAutomationPlan(String name, String agentCode, String instruction,
                                       String scheduleRule, String deliveryMode) {
        DeliveryMode mode = parseDeliveryMode(deliveryMode);
        AutomationPlan plan = automationPlanFactory.create(name, agentCode, instruction, scheduleRule, mode);
        return automationPlanRepository.save(plan).getCode();
    }

    @Override
    public void updateAutomationPlan(String planCode, String name, String instruction,
                                     String scheduleRule, String deliveryMode) {
        AutomationPlan plan = automationPlanRepository.findByCode(planCode)
                .orElseThrow(() -> new IllegalArgumentException("automation plan not found: " + planCode));

        if (name != null && !name.isBlank()) {
            plan.setName(name);
        }
        if (instruction != null && !instruction.isBlank()) {
            plan.setInstruction(instruction);
        }
        if (scheduleRule != null && !scheduleRule.isBlank()) {
            plan.setScheduleRule(scheduleRule);
        }
        if (deliveryMode != null && !deliveryMode.isBlank()) {
            plan.setDeliveryMode(parseDeliveryMode(deliveryMode));
        }
        automationPlanRepository.save(plan);
    }

    private DeliveryMode parseDeliveryMode(String value) {
        if (value == null || value.isBlank()) {
            return DeliveryMode.PUSH;
        }
        try {
            return DeliveryMode.valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return DeliveryMode.PUSH;
        }
    }
}
