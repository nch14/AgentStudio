package com.chenhaonee.agents.connect.capability;

import com.chenhaonee.agents.domain.session.model.MessageRole;

/**
 * Agent 可调用的消息写入能力。
 */
public interface MessageApi {

    /**
     * 向指定会话追加一条消息。
     */
    void appendMessage(String sessionCode, MessageRole role, String content);
}
