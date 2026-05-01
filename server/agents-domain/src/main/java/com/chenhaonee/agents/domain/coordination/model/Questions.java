package com.chenhaonee.agents.domain.coordination.model;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Questions 实体，表示由 Agent 发起的一组问题。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "questions")
@EqualsAndHashCode(callSuper = true)
public class Questions extends BaseEntity {

    @Column(name = "task_code", length = 128, nullable = false, updatable = false)
    private String taskCode;

    @Column(name = "turn_code", length = 128, nullable = false, updatable = false)
    private String turnCode;

    /** 结构化的 List<Question> 序列化 JSON */
    @Lob
    @Column(name = "questions_json", nullable = false)
    @Convert(converter = QuestionListConverter.class)
    private List<Question> questions;

    /** 问题的答案 */
    @Lob
    @Column(name = "answer_json")
    @Convert(converter = AnswerListConverter.class)
    private List<Answer> answers;

    @Column(nullable = false)
    private boolean resolved;

    @Column(nullable = false, updatable = false)
    private Instant openedAt;

    private Instant resolvedAt;

    public void initialize(String taskCode, String turnCode, List<Question> questions) {
        this.taskCode = ValidationUtils.requireText(taskCode, "taskCode");
        this.turnCode = ValidationUtils.requireText(turnCode, "turnCode");
        if (questions == null || questions.isEmpty()) {
            throw new IllegalArgumentException("questions list must not be empty");
        }
        this.questions = questions;
        this.resolved = false;
        this.openedAt = Instant.now();
    }

    public void resolve(List<Answer> answers) {
        if (this.resolved) {
            throw new IllegalStateException("questions already resolved");
        }
        this.answers = answers;
        this.resolved = true;
        this.resolvedAt = Instant.now();
    }
}
