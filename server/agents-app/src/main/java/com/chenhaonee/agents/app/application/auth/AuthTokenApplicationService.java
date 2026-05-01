package com.chenhaonee.agents.app.application.auth;

import com.chenhaonee.agents.app.infrastructure.security.AuthProperties;
import com.chenhaonee.agents.domain.auth.service.ApiTokenDomainService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthTokenApplicationService {

    private final ApiTokenDomainService apiTokenDomainService;
    private final AuthProperties authProperties;

    public AuthTokenApplicationService(ApiTokenDomainService apiTokenDomainService,
                                       AuthProperties authProperties) {
        this.apiTokenDomainService = apiTokenDomainService;
        this.authProperties = authProperties;
    }

    public String initializeFromHttp(String plainToken) {
        if (!authProperties.isAllowHttpInit()) {
            throw new IllegalStateException("HTTP token initialization is disabled");
        }
        return initializeFromBootstrap(plainToken);
    }

    public String initializeFromBootstrap(String plainToken) {
        validatePlainToken(plainToken);
        apiTokenDomainService.initialize(plainToken);
        return plainToken;
    }

    public String rotate(String plainToken) {
        validatePlainToken(plainToken);
        apiTokenDomainService.rotate(plainToken);
        return plainToken;
    }

    public boolean validate(String plainToken) {
        return apiTokenDomainService.validate(plainToken);
    }

    public boolean hasCurrentToken() {
        return apiTokenDomainService.hasValidToken();
    }

    private void validatePlainToken(String plainToken) {
        if (!StringUtils.hasText(plainToken)) {
            throw new IllegalArgumentException("API token must not be blank");
        }
        if (plainToken.length() < authProperties.effectiveTokenMinLength()) {
            throw new IllegalArgumentException("API token length must be at least "
                    + authProperties.effectiveTokenMinLength());
        }
    }
}
