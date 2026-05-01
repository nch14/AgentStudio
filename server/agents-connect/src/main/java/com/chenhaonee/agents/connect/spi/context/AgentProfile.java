package com.chenhaonee.agents.connect.spi.context;

import java.util.Map;

/**
 * Agent 级配置快照。
 */
public record AgentProfile(
        String name,
        String responsibility,
        Map<String, String> providerConfig
) {
}
