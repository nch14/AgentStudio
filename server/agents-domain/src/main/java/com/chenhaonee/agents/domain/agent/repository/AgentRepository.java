package com.chenhaonee.agents.domain.agent.repository;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.agent.model.Agent;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Agent 实例仓储。
 */
public interface AgentRepository extends JpaRepository<Agent, String> {

    Optional<Agent> findByCode(String code);

    List<Agent> findByStatus(Status status);

    Page<Agent> findByValidIsTrueOrderByUpdateTimeDesc(Pageable pageable);

    Page<Agent> findByStatusAndValidIsTrueOrderByUpdateTimeDesc(Status status, Pageable pageable);
}
