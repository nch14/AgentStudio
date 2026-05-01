package com.chenhaonee.agents.domain.session.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 会话与底层 AI Provider 会话的绑定关系。
 */
@Entity
@Data
@NoArgsConstructor
@Table(
        name = "session_relation",
        uniqueConstraints = @UniqueConstraint(columnNames = {"target_type", "target_code", "provider_type"})
)
@EqualsAndHashCode(callSuper = true)
public class SessionRelation extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, updatable = false, length = 32)
    private SessionRelationTargetType targetType;

    @Column(name = "target_code", nullable = false, updatable = false, length = 128)
    private String targetCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, updatable = false, length = 32)
    private AgentProvider providerType;

    @Column(nullable = false)
    private String providerSessionId;

    public SessionRelation(SessionRelationTargetType targetType, String targetCode,
                           AgentProvider providerType, String providerSessionId) {
        this.targetType = targetType;
        this.targetCode = targetCode;
        this.providerType = providerType;
        this.providerSessionId = providerSessionId;
    }
}
