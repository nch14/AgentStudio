package com.chenhaonee.agents.domain.task.model;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 任务的一次回合执行实例。
 */
@Entity
@Data
@NoArgsConstructor
@Table(
        name = "task_turn",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_task_turn_task_turn_no", columnNames = {"task_code", "turn_no"})
        }
)
@EqualsAndHashCode(callSuper = true)
public class TaskTurn extends BaseEntity {

    @Column(name = "task_code", length = 128, nullable = false, updatable = false)
    private String taskCode;

    @Column(name = "turn_no", nullable = false, updatable = false)
    private int turnNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TurnRunStatus runStatus;

    /**
     * 本次 turn 结束时所观察到的项目进度（0-100）。
     * 创建时由 {@code TaskTurnFactory} 用 {@code Task.progress} 作为基线写入；
     * 终结时若 Agent 提供新的评估则覆盖，否则沿用基线。
     */
    private int progress = 0;

    @Column(nullable = false, updatable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Column(columnDefinition = "TEXT")
    private String finalSummary;

    @Lob
    private String finalDetail;

    public void resumeRunning() {
        if (runStatus != TurnRunStatus.SUSPENDED) {
            throw new IllegalStateException("task turn cannot resume from status " + runStatus);
        }
        this.runStatus = TurnRunStatus.RUNNING;
    }

    public void resumeFromHanging() {
        if (runStatus != TurnRunStatus.HANGING) {
            throw new IllegalStateException("task turn cannot resume from status " + runStatus);
        }
        this.runStatus = TurnRunStatus.RUNNING;
        this.finalSummary = null;
        this.finalDetail = null;
        this.finishedAt = null;
    }

    public void waitOwner() {
        ensureRunning();
        this.runStatus = TurnRunStatus.SUSPENDED;
    }

    public void succeed(String finalSummary, String finalDetail) {
        finish(TurnRunStatus.TERMINATED, finalSummary, finalDetail, 100);
    }

    public void fail(String finalSummary, String finalDetail) {
        finish(TurnRunStatus.HANGING, finalSummary, finalDetail, null);
    }

    public void hang(String finalSummary, String finalDetail) {
        finish(TurnRunStatus.HANGING, finalSummary, finalDetail, null);
    }

    public void cancel(String finalSummary, String finalDetail) {
        if (isTerminal()) {
            throw new IllegalStateException("task turn cannot be cancelled from status " + runStatus);
        }
        finish(TurnRunStatus.CANCELLED, finalSummary, finalDetail, null);
    }

    public void timeoutCancel(String finalSummary, String finalDetail) {
        if (runStatus != TurnRunStatus.HANGING) {
            throw new IllegalStateException("task turn cannot timeout-cancel from status " + runStatus);
        }
        this.runStatus = TurnRunStatus.CANCELLED;
        this.finalSummary = ValidationUtils.requireText(finalSummary, "finalSummary");
        this.finalDetail = finalDetail;
        this.finishedAt = Instant.now();
    }

    public void completeTurn(String finalSummary, String finalDetail, Integer progress) {
        finish(TurnRunStatus.TERMINATED, finalSummary, finalDetail, progress);
    }

    public boolean isFinished() {
        return progress == 100;
    }

    private void finish(TurnRunStatus targetStatus, String finalSummary, String finalDetail, Integer progress) {
        if (runStatus != TurnRunStatus.RUNNING && runStatus != TurnRunStatus.SUSPENDED) {
            throw new IllegalStateException("task turn cannot finish from status " + runStatus);
        }
        this.runStatus = targetStatus;
        this.finalSummary = ValidationUtils.requireText(finalSummary, "finalSummary");
        this.finalDetail = finalDetail;
        if (progress != null) {
            this.progress = progress;
        }
        this.finishedAt = Instant.now();
    }

    private void ensureRunning() {
        if (runStatus != TurnRunStatus.RUNNING) {
            throw new IllegalStateException("task turn must be RUNNING but was " + runStatus);
        }
    }

    private boolean isTerminal() {
        return runStatus == TurnRunStatus.TERMINATED
                || runStatus == TurnRunStatus.HANGING
                || runStatus == TurnRunStatus.CANCELLED;
    }
}
