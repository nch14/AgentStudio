package com.chenhaonee.agents.domain.session.model;

/**
 * 会话消息 block 类型。
 */
public enum ContentBlockType {
    TEXT,
    THINKING,
    TOOL_USE,
    TOOL_RESULT,
    /** 图片附件，对齐 Anthropic image。 */
    IMAGE,
    /** 文档附件 (pdf/txt/md)，对齐 Anthropic document。 */
    DOCUMENT,
    /** Turn 级别开始标记，标志整个 agentic loop 开始执行，无内容 payload。 */
    TURN_START,
    /** Turn 级别完成标记，标志整个 agentic loop 执行结束，无内容 payload。 */
    TURN_STOP
}
