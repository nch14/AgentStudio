package com.chenhaonee.agents.app.application.schedule;

import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import com.chenhaonee.agents.domain.schedule.service.AutomationPlanDomainService;
import org.springframework.stereotype.Service;

/**
 * 自动化计划命令应用服务。
 */
@Service
public class AutomationPlanCommandApplicationService {

    private final AutomationPlanDomainService automationPlanDomainService;

    public AutomationPlanCommandApplicationService(AutomationPlanDomainService automationPlanDomainService) {
        this.automationPlanDomainService = automationPlanDomainService;
    }

    public AutomationPlan createPlan(String name, String agentCode, String instruction,
                                     String scheduleRule, DeliveryMode deliveryMode) {
        return automationPlanDomainService.createPlan(name, agentCode, instruction, scheduleRule, deliveryMode);
    }

    public AutomationPlan updatePlan(String planCode, String name, String instruction,
                                     String scheduleRule, DeliveryMode deliveryMode) {
        return automationPlanDomainService.updatePlan(planCode, name, instruction, scheduleRule, deliveryMode);
    }

    public void deletePlan(String planCode) {
        automationPlanDomainService.deletePlan(planCode);
    }

    public AutomationPlan enablePlan(String planCode) {
        return automationPlanDomainService.enable(planCode);
    }

    public AutomationPlan disablePlan(String planCode) {
        return automationPlanDomainService.disable(planCode);
    }
}
