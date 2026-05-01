package com.chenhaonee.agents.domain.coordination.model;

import java.util.List;

/**
 * 问题 VO，表示单个问题的内容与选项。
 */
public record Question(
        String code,
        String questionText,
        List<String> options
) {
}
