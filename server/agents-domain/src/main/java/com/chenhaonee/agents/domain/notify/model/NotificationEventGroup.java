package com.chenhaonee.agents.domain.notify.model;

import java.util.Arrays;

/**
 * 通知事件分组。
 */
public enum NotificationEventGroup {

    /** Agent 需要用户参与协同的通知 */
    COORDINATION("coordination", "协同通知"),
    /** Task 生命周期和执行状态通知 */
    TASK("task", "任务通知");

    private final String code;
    private final String displayName;

    NotificationEventGroup(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public static NotificationEventGroup fromCode(String code) {
        return Arrays.stream(values())
                .filter(group -> group.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知通知事件分组: " + code));
    }
}
