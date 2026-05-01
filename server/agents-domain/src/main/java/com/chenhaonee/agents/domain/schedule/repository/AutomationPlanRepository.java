package com.chenhaonee.agents.domain.schedule.repository;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 自动化任务模板仓储。
 */
public interface AutomationPlanRepository extends JpaRepository<AutomationPlan, Long> {

    List<AutomationPlan> findByStatus(Status status);

    Page<AutomationPlan> findByStatus(Status status, Pageable pageable);

    Optional<AutomationPlan> findByCode(String code);
}
