package com.chenhaonee.agents.domain.session.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Agent 会话上下文聚合。
 * 统一承载 Chat 对话与 Task 执行记录。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "agent_session")
@EqualsAndHashCode(callSuper = true)
public class AgentSession extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 128, nullable = false, updatable = false)
    private String agentCode;

    private int messageCount;

    private Instant lastMessageTime;

    /** 是否已归档 */
    private boolean archived = false;

    public void appendMessage(Instant messageTime) {
        if (messageTime == null) {
            throw new IllegalArgumentException("messageTime cannot be null");
        }
        messageCount++;
        if (lastMessageTime == null || messageTime.isAfter(lastMessageTime)) {
            this.lastMessageTime = messageTime;
        }
    }

    public void rename(String title) {
        this.title = title;
    }

    public void archive() {
        this.archived = true;
    }

    public void unarchive() {
        this.archived = false;
    }

    public void markDeleted() {
        this.valid = false;
    }
}
