package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.common.config.AgentWorkspaceProperties;
import com.chenhaonee.agents.connect.spi.core.AgentHomeInitializer;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.agent.service.AgentDomainService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Agent 命令应用服务。
 */
@Service
@RequiredArgsConstructor
public class AgentCommandApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AgentCommandApplicationService.class);

    private final AgentDomainService agentDomainService;
    private final ProviderConfigValidationService providerConfigValidationService;
    private final AgentWorkspaceProperties workspaceProperties;
    private final List<AgentHomeInitializer> initializers;

    public Agent createAgent(String name, String responsibility, AgentProvider provider, Map<String, String> providerConfig) {
        providerConfigValidationService.validate(provider, providerConfig);
        Agent agent = agentDomainService.createAgent(name, responsibility, provider, providerConfig);
        initAgentHome(agent.getCode(), agent.getProvider());
        return agent;
    }

    public Agent updateAgent(String code, String name, String responsibility, Map<String, String> providerConfig) {
        Agent agent = agentDomainService.requireAgent(code);
        providerConfigValidationService.validate(agent.getProvider(), providerConfig);
        return agentDomainService.updateAgent(code, name, responsibility, providerConfig);
    }

    public void deleteAgent(String code) {
        agentDomainService.deleteAgent(code);
    }

    public Agent enableAgent(String code) {
        return agentDomainService.enable(code);
    }

    public Agent disableAgent(String code) {
        return agentDomainService.disable(code);
    }

    private void initAgentHome(String agentCode, AgentProvider provider) {
        Path agentHome = Paths.get(workspaceProperties.getWorkspacePath(), agentCode);
        try {
            Files.createDirectories(agentHome);
            initializers.stream()
                    .filter(i -> i.supportedType() == provider)
                    .findFirst()
                    .ifPresent(i -> {
                        try {
                            i.initHome(agentCode, agentHome);
                        } catch (IOException e) {
                            log.warn("initHome failed for agent {}: {}", agentCode, e.getMessage());
                        }
                    });
        } catch (IOException e) {
            log.warn("Failed to create agent home for {}: {}", agentCode, e.getMessage());
        }
    }
}
