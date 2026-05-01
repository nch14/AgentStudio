package com.chenhaonee.agents.connect.capability;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;

/**
 * 会话管理能力。
 * <p>
 * 开放给 agent-xx-impl 实现层，用于管理 session 与底层 provider 的绑定关系。
 * 由 app 层提供实现并注册为 Spring Bean。
 */
public interface SessionApi {

    /**
     * 将指定业务目标与底层 AI Provider 会话进行绑定。
     */
    void bind(SessionRelationTargetType targetType, String targetCode,
              AgentProvider providerType, String providerSessionId);

    /**
     * 重新绑定 provider 会话。会先解绑旧的关系，再创建新的绑定。
     */
    void rebind(SessionRelationTargetType targetType, String targetCode,
                AgentProvider providerType, String newProviderSessionId);

    /**
     * 查询指定业务目标已绑定的 providerSessionId，未绑定返回 null。
     */
    String getProviderSessionId(SessionRelationTargetType targetType, String targetCode,
                                AgentProvider providerType);
}
