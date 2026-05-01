package com.chenhaonee.agents;

import com.chenhaonee.agents.app.infrastructure.security.AuthProperties;
import com.chenhaonee.agents.common.config.AgentWorkspaceProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties({AgentWorkspaceProperties.class, AuthProperties.class})
public class AgentsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AgentsApplication.class, args);
    }
}
