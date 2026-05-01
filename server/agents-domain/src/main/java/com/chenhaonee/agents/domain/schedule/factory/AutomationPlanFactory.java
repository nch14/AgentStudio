package com.chenhaonee.agents.domain.schedule.factory;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import org.springframework.stereotype.Component;

/**
 * 自动化任务模板工厂。
 */
@Component
public class AutomationPlanFactory {

    public AutomationPlan create(String name, String agentCode, String instruction,
                                 String scheduleRule, DeliveryMode deliveryMode) {
        AutomationPlan plan = new AutomationPlan();
        plan.setName(ValidationUtils.requireText(name, "name"));
        plan.setAgentCode(ValidationUtils.requireText(agentCode, "agentCode"));
        plan.setInstruction(ValidationUtils.requireText(instruction, "instruction"));
        plan.setScheduleRule(ValidationUtils.requireText(scheduleRule, "scheduleRule"));
        plan.setDeliveryMode(deliveryMode);
        plan.setStatus(Status.ENABLED);
        return plan;
    }
}
