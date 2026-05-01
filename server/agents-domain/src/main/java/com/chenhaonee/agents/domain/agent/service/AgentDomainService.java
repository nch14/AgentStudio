package com.chenhaonee.agents.domain.agent.service;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.agent.factory.AgentFactory;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.repository.AgentRepository;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 领域服务。
 */
@Service
public class AgentDomainService {

    private final AgentRepository agentRepository;
    private final AgentFactory agentFactory;

    public AgentDomainService(AgentRepository agentRepository, AgentFactory agentFactory) {
        this.agentRepository = agentRepository;
        this.agentFactory = agentFactory;
    }

    public Agent requireEnabledAgent(String code) {
        Agent agent = agentRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("agent not found: " + code));
        if (!agent.isEnabled()) {
            throw new IllegalStateException("agent is disabled: " + code);
        }
        return agent;
    }

    public Agent requireAgent(String code) {
        return agentRepository.findByCode(code)
                .orElseThrow(() -> new NoSuchElementException("agent not found: " + code));
    }

    public java.util.List<Agent> findEnabledAgents() {
        return agentRepository.findByStatus(Status.ENABLED);
    }

    public Page<Agent> listAgents(int page, int size, Status status) {
        PageRequest pageable = PageRequest.of(page, size);
        if (status != null) {
            return agentRepository.findByStatusAndValidIsTrueOrderByUpdateTimeDesc(status, pageable);
        }
        return agentRepository.findByValidIsTrueOrderByUpdateTimeDesc(pageable);
    }

    @Transactional(rollbackFor = Exception.class)
    public Agent createAgent(String name, String responsibility, AgentProvider provider, Map<String, String> providerConfig) {
        String code = null; // Factory will generate or we assign later
        Agent agent = agentFactory.create(code, name, responsibility, provider);
        if (providerConfig != null) {
            agent.setProviderConfig(providerConfig);
        }
        return agentRepository.save(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public Agent updateAgent(String code, String name, String responsibility, Map<String, String> providerConfig) {
        Agent agent = requireAgent(code);
        if (name != null && !name.isBlank()) {
            agent.setName(name);
        }
        if (responsibility != null) {
            agent.setResponsibility(responsibility);
        }
        if (providerConfig != null) {
            agent.setProviderConfig(providerConfig);
        }
        return agentRepository.save(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteAgent(String code) {
        Agent agent = requireAgent(code);
        agent.setValid(false);
        agentRepository.save(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public Agent enable(String code) {
        Agent agent = agentRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("agent not found: " + code));
        agent.enable();
        return agentRepository.save(agent);
    }

    @Transactional(rollbackFor = Exception.class)
    public Agent disable(String code) {
        Agent agent = agentRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("agent not found: " + code));
        agent.disable();
        return agentRepository.save(agent);
    }
}
