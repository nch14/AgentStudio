package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.MessageApi;
import com.chenhaonee.agents.domain.session.model.AgentMessage;
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
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setRole(role);
        message.setContent(content);
        agentSessionDomainService.appendMessage(sessionCode, message);
    }
}
