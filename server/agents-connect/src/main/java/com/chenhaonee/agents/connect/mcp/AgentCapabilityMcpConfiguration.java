package com.chenhaonee.agents.connect.mcp;

import com.chenhaonee.agents.connect.mcp.tool.CoordinationTools;
import com.chenhaonee.agents.connect.mcp.tool.TaskCommandTools;
import com.chenhaonee.agents.connect.mcp.tool.TurnReportTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MCP tool 装配配置。
 */
@Configuration
public class AgentCapabilityMcpConfiguration {

    @Bean
    ToolCallbackProvider capabilityToolCallbackProvider(CoordinationTools coordinationTools,
                                                        TaskCommandTools taskCommandTools,
                                                        TurnReportTools turnReportTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(coordinationTools, taskCommandTools, turnReportTools)
                .build();
    }
}
