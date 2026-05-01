package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class ProviderCapabilityService {

    private final Map<AgentProvider, ProviderCapabilities> capabilities;

    public ProviderCapabilityService(
            List<TaskAgent> taskAgents,
            List<MessagesAgent> messagesAgents) {
        this.capabilities = new EnumMap<>(AgentProvider.class);

        for (AgentProvider provider : AgentProvider.values()) {
            boolean hasTask = taskAgents.stream().anyMatch(a -> a.supportedType() == provider);
            boolean hasMessages = messagesAgents.stream().anyMatch(a -> a.supportedType() == provider);
            capabilities.put(provider, new ProviderCapabilities(false, hasTask, hasMessages));
        }
    }

    public ProviderCapabilities getCapability(AgentProvider provider) {
        return capabilities.get(provider);
    }

    public Map<AgentProvider, ProviderCapabilities> getAllCapabilities() {
        return capabilities;
    }

    public boolean supportsMessages(AgentProvider provider) {
        return getCapability(provider).supportMessages();
    }

    public record ProviderCapabilities(boolean supportChat, boolean supportTask, boolean supportMessages) {}
}
