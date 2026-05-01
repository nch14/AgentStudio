package com.chenhaonee.agents.app.application.schedule;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.service.AutomationPlanDomainService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * 自动化计划查询应用服务。
 */
@Service
public class AutomationPlanQueryApplicationService {

    private final AutomationPlanDomainService automationPlanDomainService;

    public AutomationPlanQueryApplicationService(AutomationPlanDomainService automationPlanDomainService) {
        this.automationPlanDomainService = automationPlanDomainService;
    }

    public Page<AutomationPlan> listPlans(int page, int size, Status status) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status != null) {
            return automationPlanDomainService.listPlans(status, pageable);
        }
        return automationPlanDomainService.listPlans(pageable);
    }

    public AutomationPlan getPlan(String planCode) {
        return automationPlanDomainService.requirePlan(planCode);
    }
}
