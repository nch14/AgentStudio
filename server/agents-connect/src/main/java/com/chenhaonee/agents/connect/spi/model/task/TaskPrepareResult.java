package com.chenhaonee.agents.connect.spi.model.task;

import java.util.Objects;

/**
 * 任务准备结果。
 * <p>
 * 仅表达 impl 是否允许启动本次 run，以及可选的 impl 会话标识。
 */
public record TaskPrepareResult(
        boolean ready,
        String failureReason
) {

    public TaskPrepareResult {
        if (!ready && (failureReason == null || failureReason.isBlank())) {
            throw new IllegalArgumentException("failureReason cannot be blank when ready is false");
        }
    }

    public static TaskPrepareResult finish() {
        return new TaskPrepareResult(true, null);
    }

    public static TaskPrepareResult failed(String failureReason) {
        Objects.requireNonNull(failureReason, "failureReason cannot be null");
        return new TaskPrepareResult(false, failureReason);
    }
}
