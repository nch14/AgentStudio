package com.chenhaonee.agents.app.application.notify;

import java.util.List;

/**
 * 通知配置视图。将系统内置事件与用户配置合并后的展示结构。
 */
public record NotifyConfigView(
        String groupCode,
        String groupName,
        String eventCode,
        String eventName,
        String description,
        boolean enabled,
        String deliveryMode,
        List<String> channels,
        boolean configured,
        String createTime,
        String updateTime) {
}
