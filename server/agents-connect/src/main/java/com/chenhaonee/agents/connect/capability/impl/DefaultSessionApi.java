package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.SessionApi;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelation;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import com.chenhaonee.agents.domain.session.service.SessionRelationDomainService;
import org.springframework.stereotype.Component;

/**
 * SessionApi 的 connect 模块实现。
 */
@Component
public class DefaultSessionApi implements SessionApi {

    private final SessionRelationDomainService sessionRelationDomainService;

    public DefaultSessionApi(SessionRelationDomainService sessionRelationDomainService) {
        this.sessionRelationDomainService = sessionRelationDomainService;
    }

    @Override
    public void bind(SessionRelationTargetType targetType, String targetCode,
                     AgentProvider providerType, String providerSessionId) {
        sessionRelationDomainService.bind(targetType, targetCode, providerType, providerSessionId);
    }

    @Override
    public void rebind(SessionRelationTargetType targetType, String targetCode,
                       AgentProvider providerType, String newProviderSessionId) {
        sessionRelationDomainService.rebind(targetType, targetCode, providerType, newProviderSessionId);
    }

    @Override
    public String getProviderSessionId(SessionRelationTargetType targetType, String targetCode,
                                       AgentProvider providerType) {
        return sessionRelationDomainService.find(targetType, targetCode, providerType)
                .map(SessionRelation::getProviderSessionId)
                .orElse(null);
    }
}
