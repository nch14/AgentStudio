package com.chenhaonee.agents.domain.session.repository;

import com.chenhaonee.agents.domain.session.model.AgentMessage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Agent 消息仓储。
 */
public interface AgentMessageRepository extends JpaRepository<AgentMessage, Long> {

    Optional<AgentMessage> findByCode(String code);

    Optional<AgentMessage> findTopBySessionCodeOrderByMessageIndexDesc(String sessionCode);

    List<AgentMessage> findBySessionCodeOrderByMessageIndexAsc(String sessionCode);

    Page<AgentMessage> findBySessionCodeOrderByMessageIndexAsc(String sessionCode, Pageable pageable);
}
