package com.chenhaonee.agents.domain.coordination.model;

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
 * Instruction 实体，表示由 用户 插入给 Agent 的指令。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "instruction")
@EqualsAndHashCode(callSuper = true)
public class Instruction extends BaseEntity {

    @Column(name = "task_code", length = 128, nullable = false, updatable = false)
    private String taskCode;

    @Column(name = "turn_code", length = 128, updatable = false)
    private String turnCode;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InstructionStatus status;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    public void initialize(String taskCode, String turnCode, String content) {
        this.taskCode = ValidationUtils.requireText(taskCode, "taskCode");
        this.turnCode = ValidationUtils.requireText(turnCode, "turnCode");
        this.content = ValidationUtils.requireText(content, "content");
        this.status = InstructionStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void accept() {
        if (this.status != InstructionStatus.PENDING) {
            throw new IllegalStateException("instruction is not pending");
        }
        this.status = InstructionStatus.ACCEPTED;
    }
}
