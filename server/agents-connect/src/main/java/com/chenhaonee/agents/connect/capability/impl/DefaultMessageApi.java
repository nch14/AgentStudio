package com.chenhaonee.agents.connect.capability.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.connect.capability.MessageApi;
import com.chenhaonee.agents.domain.session.model.ContentBlockType;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import org.springframework.stereotype.Component;

/**
 * MessageApi 的 connect 模块实现。
 */
@Component
public class DefaultMessageApi implements MessageApi {

    private final AgentSessionDomainService agentSessionDomainService;

    public DefaultMessageApi(AgentSessionDomainService agentSessionDomainService) {
        this.agentSessionDomainService = agentSessionDomainService;
    }

    @Override
    public void appendMessage(String sessionCode, MessageRole role, String content) {
        if (role == MessageRole.TOOL) {
            throw new IllegalArgumentException("MessageApi does not support TOOL role text append");
        }
        JSONObject payload = new JSONObject();
        payload.put("text", content);
        agentSessionDomainService.appendBlock(
                sessionCode,
                sessionCode,
                role,
                ContentBlockType.TEXT,
                (MessageProtocolType) null,
                JSON.toJSONString(payload),
                null
        );
    }
}
