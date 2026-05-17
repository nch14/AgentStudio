package com.chenhaonee.agents.domain.notify.model;

import com.chenhaonee.agents.common.notify.NotificationChannel;

import java.util.EnumMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 系统内置通知模板。
 */
public enum NotificationTemplate {

    QUESTIONS_RAISED_BARK(
            NotificationEvent.QUESTIONS_RAISED,
            NotificationChannel.BARK,
            "需要你回答：{taskTitle}",
            "第 {turnNo} 轮提出了 {questionCount} 个问题，请打开任务继续处理。"),
    QUESTIONS_RAISED_EMAIL(
            NotificationEvent.QUESTIONS_RAISED,
            NotificationChannel.EMAIL,
            "Agent 请求提问：{taskTitle}",
            "任务「{taskTitle}」第 {turnNo} 轮有 {questionCount} 个问题需要你回答。\n\n请打开任务详情查看并回复。"),
    TURN_WAITING_BARK(
            NotificationEvent.TURN_WAITING,
            NotificationChannel.BARK,
            "任务等待输入：{taskTitle}",
            "第 {turnNo} 轮正在等待你的输入或协同处理。"),
    TURN_WAITING_EMAIL(
            NotificationEvent.TURN_WAITING,
            NotificationChannel.EMAIL,
            "任务等待输入：{taskTitle}",
            "任务「{taskTitle}」第 {turnNo} 轮正在等待你的输入或协同处理。\n\n请打开任务详情继续处理。"),
    TURN_HANGING_BARK(
            NotificationEvent.TURN_HANGING,
            NotificationChannel.BARK,
            "任务执行异常：{taskTitle}",
            "第 {turnNo} 轮已挂起，原因：{reason}"),
    TURN_HANGING_EMAIL(
            NotificationEvent.TURN_HANGING,
            NotificationChannel.EMAIL,
            "任务执行异常：{taskTitle}",
            "任务「{taskTitle}」第 {turnNo} 轮执行过程中出现异常或挂起。\n\n原因：{reason}\n\n请检查执行链路或手动恢复。"),
    TASK_SUCCEEDED_BARK(
            NotificationEvent.TASK_SUCCEEDED,
            NotificationChannel.BARK,
            "任务已完成：{taskTitle}",
            "第 {turnNo} 轮完成。摘要：{resultSummary}"),
    TASK_SUCCEEDED_EMAIL(
            NotificationEvent.TASK_SUCCEEDED,
            NotificationChannel.EMAIL,
            "任务已完成：{taskTitle}",
            "任务「{taskTitle}」已在第 {turnNo} 轮完成。\n\n结果摘要：\n{resultSummary}");

    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\{([a-zA-Z0-9_]+)}");
    private static final Map<NotificationEvent, Map<NotificationChannel, NotificationTemplate>> INDEX = buildIndex();

    private final NotificationEvent event;
    private final NotificationChannel channel;
    private final String subjectTemplate;
    private final String contentTemplate;

    NotificationTemplate(NotificationEvent event, NotificationChannel channel,
                         String subjectTemplate, String contentTemplate) {
        this.event = event;
        this.channel = channel;
        this.subjectTemplate = subjectTemplate;
        this.contentTemplate = contentTemplate;
    }

    public NotificationEvent event() {
        return event;
    }

    public NotificationChannel channel() {
        return channel;
    }

    /**
     * 按事件和渠道渲染通知模板。
     */
    public static RenderedNotification render(NotificationEvent event, NotificationChannel channel,
                                              Map<String, String> variables) {
        if (event == null) {
            throw new IllegalArgumentException("通知事件不能为空");
        }
        if (channel == null) {
            throw new IllegalArgumentException("通知渠道不能为空");
        }
        NotificationTemplate template = INDEX.getOrDefault(event, Map.of()).get(channel);
        if (template == null) {
            throw new IllegalStateException("通知模板不存在: event=" + event.code() + ", channel=" + channel);
        }
        return template.render(variables == null ? Map.of() : variables);
    }

    private RenderedNotification render(Map<String, String> variables) {
        return new RenderedNotification(
                renderText(subjectTemplate, variables),
                renderText(contentTemplate, variables));
    }

    private static String renderText(String template, Map<String, String> variables) {
        Matcher matcher = PLACEHOLDER_PATTERN.matcher(template);
        StringBuffer rendered = new StringBuffer();
        while (matcher.find()) {
            String value = variables.get(matcher.group(1));
            matcher.appendReplacement(rendered, Matcher.quoteReplacement(value == null ? "" : value));
        }
        matcher.appendTail(rendered);
        return rendered.toString();
    }

    private static Map<NotificationEvent, Map<NotificationChannel, NotificationTemplate>> buildIndex() {
        EnumMap<NotificationEvent, Map<NotificationChannel, NotificationTemplate>> index =
                new EnumMap<>(NotificationEvent.class);
        for (NotificationTemplate template : values()) {
            Map<NotificationChannel, NotificationTemplate> channelTemplates =
                    index.computeIfAbsent(template.event, ignored -> new EnumMap<>(NotificationChannel.class));
            NotificationTemplate previous = channelTemplates.putIfAbsent(template.channel, template);
            if (previous != null) {
                throw new IllegalStateException("duplicate notification template: event="
                        + template.event.code() + ", channel=" + template.channel);
            }
        }
        EnumMap<NotificationEvent, Map<NotificationChannel, NotificationTemplate>> immutableIndex =
                new EnumMap<>(NotificationEvent.class);
        index.forEach((event, templates) -> immutableIndex.put(event, Map.copyOf(templates)));
        return Map.copyOf(immutableIndex);
    }

    public record RenderedNotification(String subject, String content) {
    }
}
