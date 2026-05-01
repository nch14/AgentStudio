package com.chenhaonee.agents.app.infrastructure.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "agents.auth")
public class AuthProperties {

    private String initialToken = "";

    private boolean allowHttpInit = false;

    private int tokenMinLength = 32;

    public String getInitialToken() {
        return initialToken;
    }

    public void setInitialToken(String initialToken) {
        this.initialToken = initialToken;
    }

    public boolean isAllowHttpInit() {
        return allowHttpInit;
    }

    public void setAllowHttpInit(boolean allowHttpInit) {
        this.allowHttpInit = allowHttpInit;
    }

    public int getTokenMinLength() {
        return tokenMinLength;
    }

    public void setTokenMinLength(int tokenMinLength) {
        this.tokenMinLength = tokenMinLength;
    }

    public int effectiveTokenMinLength() {
        return Math.max(1, tokenMinLength);
    }
}
