package com.chenhaonee.agents.connect.spi.model.task;

/**
 * 单轮 task 执行后 turn 的结果状态。
 */
public enum TaskNextAction {
    WAIT_COORDINATION,
    TERMINATED,
    HANGING
}
