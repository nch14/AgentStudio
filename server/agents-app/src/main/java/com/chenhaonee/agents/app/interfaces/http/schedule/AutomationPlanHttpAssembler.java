package com.chenhaonee.agents.app.interfaces.http.schedule;

import com.chenhaonee.agents.app.interfaces.http.schedule.dto.AutomationPlanDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.schedule.dto.AutomationPlanListItemDTO;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import org.springframework.stereotype.Component;

/**
 * 自动化计划 HTTP DTO 装配器。
 */
@Component
public class AutomationPlanHttpAssembler {

    public AutomationPlanListItemDTO toListItemResponse(AutomationPlan plan) {
        return new AutomationPlanListItemDTO(
                plan.getCode(),
                plan.getName(),
                plan.getAgentCode(),
                plan.getScheduleRule(),
                plan.getDeliveryMode() != null ? plan.getDeliveryMode().name() : null,
                plan.getStatus() != null ? plan.getStatus().name() : null,
                plan.getCreateTime() != null ? plan.getCreateTime().toString() : null,
                plan.getUpdateTime() != null ? plan.getUpdateTime().toString() : null);
    }

    public AutomationPlanDetailDTO toDetailResponse(AutomationPlan plan) {
        return new AutomationPlanDetailDTO(
                plan.getCode(),
                plan.getName(),
                plan.getAgentCode(),
                plan.getInstruction(),
                plan.getScheduleRule(),
                plan.getDeliveryMode() != null ? plan.getDeliveryMode().name() : null,
                plan.getStatus() != null ? plan.getStatus().name() : null,
                plan.getCreateTime() != null ? plan.getCreateTime().toString() : null,
                plan.getUpdateTime() != null ? plan.getUpdateTime().toString() : null);
    }
}
