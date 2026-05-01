package com.chenhaonee.agents.domain.session.service;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.factory.SessionRelationDomainFactory;
import com.chenhaonee.agents.domain.session.model.SessionRelation;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import com.chenhaonee.agents.domain.session.repository.SessionRelationRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionRelationDomainServiceTest {

    @Mock
    private SessionRelationRepository sessionRelationRepository;

    @Mock
    private SessionRelationDomainFactory sessionRelationDomainFactory;

    @InjectMocks
    private SessionRelationDomainService sessionRelationDomainService;

    @Test
    void shouldRebindExactTargetTypeAndProviderType() {
        SessionRelation existing = new SessionRelation(
                SessionRelationTargetType.TASK_TURN,
                "same-code",
                AgentProvider.CLAUDE_CODE,
                "provider-old"
        );
        SessionRelation rebound = new SessionRelation(
                SessionRelationTargetType.TASK_TURN,
                "same-code",
                AgentProvider.CLAUDE_CODE,
                "provider-new"
        );
        when(sessionRelationRepository.findByTargetTypeAndTargetCodeAndProviderType(
                SessionRelationTargetType.TASK_TURN, "same-code", AgentProvider.CLAUDE_CODE
        )).thenReturn(Optional.of(existing));
        when(sessionRelationDomainFactory.create(
                SessionRelationTargetType.TASK_TURN, "same-code", AgentProvider.CLAUDE_CODE, "provider-new"
        )).thenReturn(rebound);
        when(sessionRelationRepository.save(rebound)).thenReturn(rebound);

        SessionRelation result = sessionRelationDomainService.rebind(
                SessionRelationTargetType.TASK_TURN,
                "same-code",
                AgentProvider.CLAUDE_CODE,
                "provider-new"
        );

        assertSame(rebound, result);
        verify(sessionRelationRepository).findByTargetTypeAndTargetCodeAndProviderType(
                SessionRelationTargetType.TASK_TURN, "same-code", AgentProvider.CLAUDE_CODE
        );
        verify(sessionRelationRepository).delete(existing);
        verify(sessionRelationDomainFactory).create(
                SessionRelationTargetType.TASK_TURN, "same-code", AgentProvider.CLAUDE_CODE, "provider-new"
        );
        verify(sessionRelationRepository).save(rebound);
    }

    @Test
    void shouldFindByTypedTargetAndProviderTuple() {
        SessionRelation relation = new SessionRelation(
                SessionRelationTargetType.AGENT_SESSION,
                "same-code",
                AgentProvider.CLAUDE_CODE,
                "provider-session-1"
        );
        when(sessionRelationRepository.findByTargetTypeAndTargetCodeAndProviderType(
                SessionRelationTargetType.AGENT_SESSION, "same-code", AgentProvider.CLAUDE_CODE
        )).thenReturn(Optional.of(relation));

        Optional<SessionRelation> result = sessionRelationDomainService.find(
                SessionRelationTargetType.AGENT_SESSION,
                "same-code",
                AgentProvider.CLAUDE_CODE
        );

        assertSame(relation, result.orElseThrow());
        verify(sessionRelationRepository).findByTargetTypeAndTargetCodeAndProviderType(
                SessionRelationTargetType.AGENT_SESSION, "same-code", AgentProvider.CLAUDE_CODE
        );
        verify(sessionRelationRepository, never()).findByTargetTypeAndTargetCodeAndProviderType(
                SessionRelationTargetType.TASK_TURN, "same-code", AgentProvider.CLAUDE_CODE
        );
    }
}
