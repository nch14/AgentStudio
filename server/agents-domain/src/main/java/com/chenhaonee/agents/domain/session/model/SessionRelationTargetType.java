package com.chenhaonee.agents.domain.session.model;

/**
 * SessionRelation 绑定目标类型。
 */
public enum SessionRelationTargetType {
    AGENT_SESSION,
    TASK,

    /**
     * @deprecated 历史 Task Turn 维度绑定。新 Task 模式按 TASK 维度绑定 provider session。
     */
    @Deprecated
    TASK_TURN
}
