package com.chenhaonee.agents.connect.capability.impl;

import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.spi.context.AgentProfile;
import com.chenhaonee.agents.connect.spi.context.OwnerContext;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.repository.AgentRepository;
import com.chenhaonee.agents.domain.profile.repository.OwnerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * AgentConfigApi 的 connect 模块实现。
 */
@Component
@RequiredArgsConstructor
public class DefaultAgentConfigApi implements AgentConfigApi {

    private final AgentRepository agentRepository;
    private final OwnerProfileRepository ownerProfileRepository;

    @Override
    public AgentProfile getAgentProfile(String agentCode) {
        Agent agent = agentRepository.findByCode(agentCode)
                .orElseThrow(() -> new IllegalArgumentException("agent not found: " + agentCode));

        return new AgentProfile(
                agent.getName(),
                agent.getResponsibility(),
                agent.getProviderConfig());
    }

    @Override
    public OwnerContext getOwnerContext() {
        return ownerProfileRepository.findFirstByOrderByIdAsc()
                .map(p -> new OwnerContext(p.getDisplayName(), p.getEmail(), p.getTimezone(), p.getLocale(), p.getBarkDeviceKey(), p.getBio()))
                .orElse(new OwnerContext("Boss", null, "Asia/Shanghai", "zh-CN", null, null));
    }

    @Override
    public Map<String, String> getProviderConfig(String agentCode) {
        return agentRepository.findByCode(agentCode)
                .map(Agent::getProviderConfig)
                .orElse(null);
    }
}
