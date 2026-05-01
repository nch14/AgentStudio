package com.chenhaonee.agents.domain.auth;

public class TokenAlreadyInitializedException extends RuntimeException {
    public TokenAlreadyInitializedException() {
        super("API token has already been initialized");
    }
}
