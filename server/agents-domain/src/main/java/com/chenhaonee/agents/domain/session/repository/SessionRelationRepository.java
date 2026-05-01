package com.chenhaonee.agents.domain.session.repository;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelation;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 会话绑定关系仓储。
 */
public interface SessionRelationRepository extends JpaRepository<SessionRelation, Long> {

    Optional<SessionRelation> findByTargetTypeAndTargetCodeAndProviderType(
            SessionRelationTargetType targetType,
            String targetCode,
            AgentProvider providerType
    );

    boolean existsByTargetTypeAndTargetCodeAndProviderType(
            SessionRelationTargetType targetType,
            String targetCode,
            AgentProvider providerType
    );
}
