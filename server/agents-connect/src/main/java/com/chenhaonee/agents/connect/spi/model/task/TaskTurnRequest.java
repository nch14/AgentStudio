package com.chenhaonee.agents.connect.spi.model.task;

import java.util.Objects;

/**
 * 单轮 task 执行请求。
 * 仅传递稳定标识，由 impl 自行恢复运行态上下文。
 */
public record TaskTurnRequest(
        String taskCode,
        String turnCode,
        String sessionCode
) {

    public TaskTurnRequest {
        Objects.requireNonNull(taskCode, "taskCode cannot be null");
        Objects.requireNonNull(turnCode, "turnCode cannot be null");
        Objects.requireNonNull(sessionCode, "sessionCode cannot be null");
        if (taskCode.isBlank()) {
            throw new IllegalArgumentException("taskCode cannot be blank");
        }
        if (turnCode.isBlank()) {
            throw new IllegalArgumentException("turnCode cannot be blank");
        }
        if (sessionCode.isBlank()) {
            throw new IllegalArgumentException("sessionCode cannot be blank");
        }
    }
}
