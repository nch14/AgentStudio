package com.chenhaonee.agents.common.notify;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Bark 推送通知器实现。
 */
@Component
@Slf4j
public class BarkNotifier implements Notifier {

    private final RestTemplate restTemplate;
    private final String serverUrl;

    public BarkNotifier(@Value("${app.bark.server-url:https://api.day.app}") String serverUrl) {
        this.restTemplate = new RestTemplate();
        this.serverUrl = serverUrl;
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.BARK;
    }

    @Override
    public void send(String recipient, String subject, String content) {
        String url = serverUrl + "/" + recipient;
        Map<String, String> body = Map.of(
                "title", subject,
                "body", content
        );
        restTemplate.postForObject(url, body, String.class);
        log.info("bark sent to={}, subject={}", recipient, subject);
    }
}
