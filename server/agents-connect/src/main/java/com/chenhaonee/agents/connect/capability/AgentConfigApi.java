package com.chenhaonee.agents.connect.capability;

import com.chenhaonee.agents.connect.spi.context.AgentProfile;
import com.chenhaonee.agents.connect.spi.context.OwnerContext;

import java.util.Map;

/**
 * Agent 配置查询 API。
 * <p>
 * 开放给 agent-xx-impl 实现层，用于按需查询 Agent 的人设、配置、Owner 信息等。
 * 由 app 层提供实现并注册为 Spring Bean。
 */
public interface AgentConfigApi {

    /** 获取 Agent 的完整配置快照 */
    AgentProfile getAgentProfile(String agentCode);

    /** 获取当前 Owner 的上下文信息 */
    OwnerContext getOwnerContext();

    /** 获取指定 Agent 的 providerConfig */
    Map<String, String> getProviderConfig(String agentCode);
}
