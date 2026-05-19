package com.chenhaonee.agents.domain.notify.model;

import java.util.Arrays;

/**
 * 系统内置通知事件。
 */
public enum NotificationEvent {

    /** Agent 请求用户回答问题 */
    QUESTIONS_RAISED(
            NotificationEventGroup.COORDINATION,
            "questions_raised",
            "Agent 请求提问",
            "Agent 在执行过程中向用户发起问题，需要用户回复"),

    /** Task turn 等待用户输入 */
    TURN_WAITING(
            NotificationEventGroup.TASK,
            "turn_waiting",
            "任务等待输入",
            "任务进入等待用户输入或协同处理状态"),

    /** Task turn 执行异常或挂起 */
    TURN_HANGING(
            NotificationEventGroup.TASK,
            "turn_hanging",
            "任务执行异常",
            "任务执行异常或进入挂起状态，需要检查执行链路"),

    /** Task 已成功完成 */
    TASK_SUCCEEDED(
            NotificationEventGroup.TASK,
            "task_succeeded",
            "任务已完成",
            "任务已成功完成并生成最终摘要");

    private final NotificationEventGroup group;
    private final String code;
    private final String displayName;
    private final String description;

    NotificationEvent(NotificationEventGroup group, String code, String displayName, String description) {
        this.group = group;
        this.code = code;
        this.displayName = displayName;
        this.description = description;
    }

    public NotificationEventGroup group() {
        return group;
    }

    public String code() {
        return code;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public static NotificationEvent fromCode(String code) {
        return Arrays.stream(values())
                .filter(event -> event.code.equals(code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未知通知事件: " + code));
    }
}
