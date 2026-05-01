package com.chenhaonee.agents.app.infrastructure.agent;

import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.spi.context.AgentProfile;
import com.chenhaonee.agents.connect.spi.context.OwnerContext;
import com.chenhaonee.agents.connect.spi.tool.ToolRegistry;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.repository.AgentRepository;
import com.chenhaonee.agents.domain.profile.repository.OwnerProfileRepository;
import java.util.Map;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * Agent 配置查询 API 的 app 层实现。
 */
@Component
@Primary
public class SpringAgentConfigApi implements AgentConfigApi {

    private final AgentRepository agentRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final ToolRegistry toolRegistry;

    public SpringAgentConfigApi(AgentRepository agentRepository,
                                OwnerProfileRepository ownerProfileRepository,
                                ToolRegistry toolRegistry) {
        this.agentRepository = agentRepository;
        this.ownerProfileRepository = ownerProfileRepository;
        this.toolRegistry = toolRegistry;
    }

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
