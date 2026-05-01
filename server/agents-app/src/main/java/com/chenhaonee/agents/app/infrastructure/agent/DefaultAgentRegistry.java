package com.chenhaonee.agents.app.infrastructure.agent;

import com.chenhaonee.agents.connect.spi.core.ChatAgent;
import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.connect.driver.AgentRegistry;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import org.springframework.stereotype.Component;

/**
 * 基于 Spring Bean 自动装配的 Agent 注册表。
 */
@Component
public class DefaultAgentRegistry implements AgentRegistry {

    private final Map<AgentProvider, ChatAgent> chatAgents;
    private final Map<AgentProvider, MessagesAgent> messagesAgents;
    private final Map<AgentProvider, TaskAgent> taskAgents;

    public DefaultAgentRegistry(
            List<ChatAgent> chatAgents,
            List<MessagesAgent> messagesAgents,
            List<TaskAgent> taskAgents
    ) {
        this.chatAgents = indexChatAgents(chatAgents);
        this.messagesAgents = indexMessagesAgents(messagesAgents);
        this.taskAgents = indexTaskAgents(taskAgents);
    }

    @Override
    public Optional<ChatAgent> findChatAgent(AgentProvider type) {
        return Optional.ofNullable(chatAgents.get(type));
    }

    @Override
    public Optional<MessagesAgent> findMessagesAgent(AgentProvider type) {
        return Optional.ofNullable(messagesAgents.get(type));
    }

    @Override
    public Optional<TaskAgent> findTaskAgent(AgentProvider type) {
        return Optional.ofNullable(taskAgents.get(type));
    }

    private Map<AgentProvider, ChatAgent> indexChatAgents(List<ChatAgent> agents) {
        Map<AgentProvider, ChatAgent> index = new EnumMap<>(AgentProvider.class);
        for (ChatAgent agent : agents) {
            index.putIfAbsent(agent.supportedType(), agent);
        }
        return index;
    }

    private Map<AgentProvider, MessagesAgent> indexMessagesAgents(List<MessagesAgent> agents) {
        Map<AgentProvider, MessagesAgent> index = new EnumMap<>(AgentProvider.class);
        for (MessagesAgent agent : agents) {
            index.putIfAbsent(agent.supportedType(), agent);
        }
        return index;
    }

    private Map<AgentProvider, TaskAgent> indexTaskAgents(List<TaskAgent> agents) {
        Map<AgentProvider, TaskAgent> index = new EnumMap<>(AgentProvider.class);
        for (TaskAgent agent : agents) {
            index.putIfAbsent(agent.supportedType(), agent);
        }
        return index;
    }
}
