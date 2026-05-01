package com.chenhaonee.agents.connect.spi.context;

/**
 * Owner（用户）上下文快照。
 */
public record OwnerContext(
        String displayName,
        String email,
        String timezone,
        String locale,
        String barkDeviceKey,
        String bio
) {
}
