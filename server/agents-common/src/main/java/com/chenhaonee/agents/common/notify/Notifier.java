package com.chenhaonee.agents.common.notify;

/**
 * 通知器 SPI。各渠道实现此接口以接入消息中心。
 */
public interface Notifier {

    /** 返回本通知器支持的渠道类型 */
    NotificationChannel getChannel();

    /**
     * 发送通知。
     *
     * @param recipient 接收者地址
     * @param subject   主题
     * @param content   内容
     */
    void send(String recipient, String subject, String content);
}
