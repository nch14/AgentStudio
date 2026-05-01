package com.chenhaonee.agents.common.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 跨 SPI 共享的 workspace 根目录配置。
 */
@Data
@ConfigurationProperties(prefix = "agents")
public class AgentWorkspaceProperties {

    private String workspacePath = System.getProperty("user.home") + "/agent-workspace";
}
