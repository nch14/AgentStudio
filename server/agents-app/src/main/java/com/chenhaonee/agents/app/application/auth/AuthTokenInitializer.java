package com.chenhaonee.agents.app.application.auth;

import com.chenhaonee.agents.app.infrastructure.security.AuthProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class AuthTokenInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthTokenInitializer.class);

    private final AuthTokenApplicationService authTokenApplicationService;
    private final AuthProperties authProperties;

    public AuthTokenInitializer(AuthTokenApplicationService authTokenApplicationService,
                                AuthProperties authProperties) {
        this.authTokenApplicationService = authTokenApplicationService;
        this.authProperties = authProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (authTokenApplicationService.hasCurrentToken()) {
            log.info("API token already initialized, bootstrap token is ignored");
            return;
        }
        if (!StringUtils.hasText(authProperties.getInitialToken())) {
            log.warn("API token is not initialized. Set AGENTS_AUTH_INITIAL_TOKEN before exposing the service");
            return;
        }
        authTokenApplicationService.initializeFromBootstrap(authProperties.getInitialToken());
        log.info("API token initialized from bootstrap configuration");
    }
}
