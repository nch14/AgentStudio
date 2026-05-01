package com.chenhaonee.agents.domain.session.factory;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelation;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import org.springframework.stereotype.Component;

/**
 * 会话绑定关系领域工厂。
 */
@Component
public class SessionRelationDomainFactory {

    public SessionRelation create(SessionRelationTargetType targetType, String targetCode,
                                  AgentProvider providerType, String providerSessionId) {
        return new SessionRelation(targetType, targetCode, providerType, providerSessionId);
    }
}
