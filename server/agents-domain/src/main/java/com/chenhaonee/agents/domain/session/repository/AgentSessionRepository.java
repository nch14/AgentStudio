package com.chenhaonee.agents.domain.session.repository;

import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

/**
 * Agent 会话仓储。
 */
public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {

    Optional<AgentSession> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<AgentSession> findWithLockByCode(String code);

    Optional<AgentSession> findByCodeAndAgentCode(String code, String agentCode);

    boolean existsByCode(String code);

    Page<AgentSession> findBySceneAndArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(SessionScene scene, Pageable pageable);

    Page<AgentSession> findBySceneAndArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(SessionScene scene, Pageable pageable);

    Page<AgentSession> findByAgentCodeAndSceneAndArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(String agentCode, SessionScene scene, Pageable pageable);

    Page<AgentSession> findByAgentCodeAndSceneAndArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(String agentCode, SessionScene scene, Pageable pageable);
}
