package com.chenhaonee.agents.connect.spi.core;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;

import java.util.List;

/**
 * Agent 实现方声明自身支持的 providerConfig 配置项。
 * <p>
 * 每种 impl 注册为 Spring Bean，app 层可据此做配置校验和 UI 展示。
 */
public interface AgentProviderConfigDescriptor {

    /** 对应的 provider 类型 */
    AgentProvider supportedType();

    /** 声明支持的配置项列表 */
    List<ConfigKeyDescriptor> supportedConfigKeys();

    /**
     * 配置项描述。
     */
    record ConfigKeyDescriptor(
            String key,
            String displayName,
            String description,
            boolean required,
            String defaultValue
    ) {
    }
}
