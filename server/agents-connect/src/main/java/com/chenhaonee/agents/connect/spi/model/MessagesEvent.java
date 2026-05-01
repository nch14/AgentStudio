package com.chenhaonee.agents.connect.spi.model;

/**
 * Messages API 的内部标准事件表示。
 *
 * @param eventType Anthropic SSE event name
 * @param dataJson  对应 event 的 JSON data
 */
public record MessagesEvent(String eventType, String dataJson) {
}
