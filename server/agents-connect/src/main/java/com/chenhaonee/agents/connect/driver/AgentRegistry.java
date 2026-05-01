package com.chenhaonee.agents.connect.driver;

import com.chenhaonee.agents.connect.spi.core.ChatAgent;
import com.chenhaonee.agents.connect.spi.core.MessagesAgent;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;

import java.util.Optional;

/**
 * Agent 实现注册与路由 SPI。
 * <p>
 * 负责根据 AgentProviderType 查找对应的 ChatAgent 或 TaskAgent 实现。
 */
public interface AgentRegistry {

    Optional<ChatAgent> findChatAgent(AgentProvider type);

    Optional<MessagesAgent> findMessagesAgent(AgentProvider type);

    Optional<TaskAgent> findTaskAgent(AgentProvider type);
}
