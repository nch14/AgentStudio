package com.chenhaonee.agents.domain.session.service;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.factory.SessionRelationDomainFactory;
import com.chenhaonee.agents.domain.session.model.SessionRelation;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import com.chenhaonee.agents.domain.session.repository.SessionRelationRepository;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 会话绑定关系领域服务。
 */
@Service
public class SessionRelationDomainService {

    private final SessionRelationRepository sessionRelationRepository;
    private final SessionRelationDomainFactory sessionRelationDomainFactory;

    public SessionRelationDomainService(
            SessionRelationRepository sessionRelationRepository,
            SessionRelationDomainFactory sessionRelationDomainFactory
    ) {
        this.sessionRelationRepository = sessionRelationRepository;
        this.sessionRelationDomainFactory = sessionRelationDomainFactory;
    }

    /** 绑定一个 providerSessionId 到指定业务目标。 */
    @Transactional(rollbackFor = Exception.class)
    public SessionRelation bind(SessionRelationTargetType targetType, String targetCode,
                                AgentProvider providerType, String providerSessionId) {
        SessionRelation relation = sessionRelationDomainFactory.create(
                targetType, targetCode, providerType, providerSessionId);
        return sessionRelationRepository.save(relation);
    }

    /** 重新绑定 providerSessionId。先解绑旧的，再绑定新的。 */
    @Transactional(rollbackFor = Exception.class)
    public SessionRelation rebind(SessionRelationTargetType targetType, String targetCode,
                                  AgentProvider providerType, String newProviderSessionId) {
        sessionRelationRepository.findByTargetTypeAndTargetCodeAndProviderType(targetType, targetCode, providerType)
                .ifPresent(sessionRelationRepository::delete);
        return bind(targetType, targetCode, providerType, newProviderSessionId);
    }

    /** 查询指定业务目标的绑定关系。 */
    public Optional<SessionRelation> find(
            SessionRelationTargetType targetType,
            String targetCode,
            AgentProvider providerType
    ) {
        return sessionRelationRepository.findByTargetTypeAndTargetCodeAndProviderType(
                targetType, targetCode, providerType);
    }
}
