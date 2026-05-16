package com.chenhaonee.agents.domain.session.factory;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Agent 会话工厂。
 */
@Component
public class AgentSessionDomainFactory {

    public AgentSession create(String title, String agentCode, SessionScene scene) {
        AgentSession session = new AgentSession();
        session.setTitle(ValidationUtils.requireText(title, "title"));
        session.setAgentCode(ValidationUtils.requireText(agentCode, "agentCode"));
        session.setScene(Objects.requireNonNull(scene, "scene"));
        return session;
    }

    public AgentSession createWithCode(String code, String title, String agentCode, SessionScene scene) {
        AgentSession session = create(title, agentCode, scene);
        session.setCode(ValidationUtils.requireText(code, "code"));
        return session;
    }
}
