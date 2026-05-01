package com.chenhaonee.agents.domain.coordination.model;

/**
 * 问题答案 VO。
 */
public record Answer(
        String questionCode,
        String selectedOption,
        String userInput
) {
}
