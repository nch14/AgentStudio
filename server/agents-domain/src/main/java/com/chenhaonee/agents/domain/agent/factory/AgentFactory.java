package com.chenhaonee.agents.domain.agent.factory;

import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import org.springframework.stereotype.Component;

/**
 * Agent 聚合工厂。
 */
@Component
public class AgentFactory {

    public Agent create(String code, String name, String responsibility, AgentProvider provider) {
        Agent agent = new Agent();
        agent.setCode(code);
        agent.setName(name);
        agent.setResponsibility(responsibility);
        agent.setProvider(provider);
        agent.setStatus(com.chenhaonee.agents.common.domain.Status.ENABLED);
        return agent;
    }
}
