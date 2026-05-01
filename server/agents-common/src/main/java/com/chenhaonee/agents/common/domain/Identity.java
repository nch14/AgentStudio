package com.chenhaonee.agents.common.domain;

import java.util.Objects;
import java.util.UUID;

/**
 * 跨模块复用的实体标识。
 */
public record Identity(String value) {

    public Identity {
        Objects.requireNonNull(value, "identity value cannot be null");
    }

    public static Identity newIdentity() {
        return new Identity(UUID.randomUUID().toString());
    }
}
