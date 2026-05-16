package com.chenhaonee.agents.domain.session.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

    /** 会话场景：CHAT 对话 / TASK 任务 */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private SessionScene scene;

    private int messageCount;

    private int turnCount;

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

    /**
     * 新轮次首次写入时调用，增加轮次计数。
     */
    public void incrementTurnCount() {
        turnCount++;
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
