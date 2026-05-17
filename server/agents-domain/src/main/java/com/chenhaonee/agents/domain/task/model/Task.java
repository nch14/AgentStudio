package com.chenhaonee.agents.domain.task.model;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 单实例内的任务聚合根。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "task")
@EqualsAndHashCode(callSuper = true)
public class Task extends BaseEntity {

    @Column(nullable = false)
    private String title;

    @Column(length = 128, nullable = false)
    private String agentCode;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskSource source;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Column(length = 128)
    private String currentTurnCode;

    /** 关联的 AgentSession.code，首次执行时设置 */
    @Column(length = 128)
    private String sessionCode;

    private Instant finishedAt;

    @Column(columnDefinition = "TEXT")
    private String resultSummary;

    /** 任务整体完成进度，0-100 百分比值 */
    @Column(nullable = false)
    private int progress;

    /**
     * 创建来源的具体实体 code，与 source 枚举配合使用。
     * SCHEDULED_CREATE 时为 AutomationPlan.code，AGENT_CREATE 时为 Agent.code。
     */
    @Column(length = 128)
    private String sourceRef;

    /**
     * 为任务绑定新的运行实例，并重置执行态字段。
     */
    public void startTurn(String turnCode) {
        if (status != TaskStatus.CREATED && status != TaskStatus.RUNNING) {
            throw new IllegalStateException("task cannot start a new turn from status " + status);
        }
        this.currentTurnCode = ValidationUtils.requireText(turnCode, "turnCode");
        this.status = TaskStatus.CREATED;
        this.finishedAt = null;
        this.resultSummary = null;
    }

    /**
     * 任务进入实际执行态。
     */
    public void markRunning() {
        ensureStatus(TaskStatus.CREATED);
        this.status = TaskStatus.RUNNING;
    }

    /**
     * 更新任务整体完成进度，0-100。
     */
    public void updateProgress(int progress) {
        if (progress < 0 || progress > 100) {
            throw new IllegalArgumentException("progress must be between 0 and 100");
        }
        this.progress = progress;
    }

    public void succeed(String resultSummary) {
        finish(TaskStatus.SUCCEEDED, resultSummary);
    }

    public void cancel(String resultSummary) {
        if (isTerminal()) {
            throw new IllegalStateException("task cannot be cancelled from status " + status);
        }
        if (status == TaskStatus.CREATED) {
            String safeSummary = ValidationUtils.requireText(resultSummary, "resultSummary");
            this.status = TaskStatus.CANCELLED;
            this.resultSummary = safeSummary;
            this.finishedAt = Instant.now();
            return;
        }
        finish(TaskStatus.CANCELLED, resultSummary);
    }

    private void ensureStatus(TaskStatus expectedStatus) {
        if (status != expectedStatus) {
            throw new IllegalStateException("task status must be " + expectedStatus + " but was " + status);
        }
    }


    private void finish(TaskStatus targetStatus, String resultSummary) {
        if (status != TaskStatus.RUNNING) {
            throw new IllegalStateException("task cannot finish from status " + status);
        }
        String safeSummary = ValidationUtils.requireText(resultSummary, "resultSummary");
        this.status = targetStatus;
        this.resultSummary = safeSummary;
        this.finishedAt = Instant.now();
    }

    /**
     * 将已终止的任务重置为 CREATED，以支持重新执行。
     */
    public void retry() {
        if (!isTerminal()) {
            throw new IllegalStateException("task cannot be retried from status " + status);
        }
        this.status = TaskStatus.CREATED;
        this.finishedAt = null;
        this.resultSummary = null;
    }

    /**
     * 将已终止的任务回滚为 RUNNING，以支持人工干预后继续执行。
     */
    public void rollback() {
        if (!isTerminal()) {
            throw new IllegalStateException("task cannot be rolled back from status " + status);
        }
        this.status = TaskStatus.RUNNING;
        this.finishedAt = null;
        this.resultSummary = null;
    }

    public boolean isTerminal() {
        return status == TaskStatus.SUCCEEDED || status == TaskStatus.CANCELLED;
    }
}
