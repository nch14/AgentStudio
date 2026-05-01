package com.chenhaonee.agents.domain.session.repository;

import com.chenhaonee.agents.domain.session.model.AgentSession;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Agent 会话仓储。
 */
public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {

    Optional<AgentSession> findByCode(String code);

    Optional<AgentSession> findByCodeAndAgentCode(String code, String agentCode);

    boolean existsByCode(String code);

    Page<AgentSession> findByArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(Pageable pageable);

    Page<AgentSession> findByArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(Pageable pageable);

    Page<AgentSession> findByAgentCodeAndArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(String agentCode, Pageable pageable);

    Page<AgentSession> findByAgentCodeAndArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(String agentCode, Pageable pageable);
}
