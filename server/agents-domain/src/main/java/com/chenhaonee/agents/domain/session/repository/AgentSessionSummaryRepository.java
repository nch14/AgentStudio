package com.chenhaonee.agents.domain.session.repository;

import com.chenhaonee.agents.domain.session.model.AgentSummary;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Agent 会话摘要仓储。
 */
public interface AgentSessionSummaryRepository extends JpaRepository<AgentSummary, Long> {

    Optional<AgentSummary> findByCode(String code);

    Optional<AgentSummary> findBySessionCode(String sessionCode);
}
