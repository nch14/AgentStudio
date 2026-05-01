package com.chenhaonee.agents.domain.session.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 会话摘要。
 * <p>
 * 作为会话衍生视图独立建模，避免与原始会话、消息事实混杂。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "agent_session_summary")
@EqualsAndHashCode(callSuper = true)
public class AgentSummary extends BaseEntity {

    @Column(name = "session_code", length = 128, nullable = false, unique = true, updatable = false)
    private String sessionCode;

    @Column(name = "latest_message_code", length = 128)
    private String latestMessageCode;

    @Column(length = 500)
    private String content;

    private boolean edited;
}
