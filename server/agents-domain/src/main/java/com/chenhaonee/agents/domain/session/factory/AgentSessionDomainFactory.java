package com.chenhaonee.agents.domain.session.factory;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import org.springframework.stereotype.Component;

/**
 * Agent 会话工厂。
 */
@Component
public class AgentSessionDomainFactory {

    public AgentSession create(String title, String agentCode) {
        AgentSession session = new AgentSession();
        session.setTitle(ValidationUtils.requireText(title, "title"));
        session.setAgentCode(ValidationUtils.requireText(agentCode, "agentCode"));
        return session;
    }
}
