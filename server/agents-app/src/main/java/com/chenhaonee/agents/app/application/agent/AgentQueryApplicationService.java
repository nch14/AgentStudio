package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

/**
 * Agent 查询应用服务。
 */
@Service
public class AgentQueryApplicationService {

    private final AgentDomainService agentDomainService;

    public AgentQueryApplicationService(AgentDomainService agentDomainService) {
        this.agentDomainService = agentDomainService;
    }

    public Page<Agent> listAgents(int page, int size, Status status) {
        return agentDomainService.listAgents(page, size, status);
    }

    public Agent getAgentByCode(String code) {
        return agentDomainService.requireAgent(code);
    }
}
