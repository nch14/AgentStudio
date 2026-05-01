package com.chenhaonee.agents.domain.auth;

public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException() {
        super("Invalid API token");
    }
}
