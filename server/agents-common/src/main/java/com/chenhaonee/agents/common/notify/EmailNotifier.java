package com.chenhaonee.agents.common.notify;

import com.chenhaonee.agents.common.notify.NotificationChannel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 邮件通知器实现。
 */
@Component
@Slf4j
@ConditionalOnClass(JavaMailSender.class)
@ConditionalOnBean(JavaMailSender.class)
public class EmailNotifier implements Notifier {

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public EmailNotifier(JavaMailSender mailSender,
                         @Value("${spring.mail.username}") String mailUsername,
                         @Value("${app.mail.sender-name:}") String senderName) {
        this.mailSender = mailSender;
        if (StringUtils.isNotBlank(senderName)) {
            this.fromAddress = senderName + " <" + mailUsername + ">";
        } else {
            this.fromAddress = mailUsername;
        }
    }

    @Override
    public NotificationChannel getChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public void send(String recipient, String subject, String content) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(content);
        mailSender.send(message);
        log.info("email sent to={}, subject={}", recipient, subject);
    }
}
