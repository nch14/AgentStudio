package com.chenhaonee.agents.app.scheduler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.app.application.scheduler.NotifyScheduler;
import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.common.notify.Notifier;
import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.model.Notification.NotificationStatus;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class NotifySchedulerTest {

    private final MessageCenter messageCenter = mock(MessageCenter.class);
    private final Notifier emailNotifier = mock(Notifier.class);

    @Test
    void shouldSendNotificationsSeparatelyForDifferentRecipients() {
        NotifyScheduler notifyScheduler = new NotifyScheduler(messageCenter);
        ReflectionTestUtils.setField(notifyScheduler, "enabled", true);

        Notification first = pendingNotification("notice-1", "first@example.com", "第一条");
        Notification second = pendingNotification("notice-2", "second@example.com", "第二条");
        when(messageCenter.getPendingNotifications()).thenReturn(List.of(first, second));
        when(messageCenter.getNotifier(NotificationChannel.EMAIL)).thenReturn(emailNotifier);

        notifyScheduler.schedule();

        verify(emailNotifier).send("first@example.com", "第一条", "内容-第一条");
        verify(emailNotifier).send("second@example.com", "第二条", "内容-第二条");
        verify(messageCenter, times(2)).saveAll(org.mockito.ArgumentMatchers.anyList());
    }

    private Notification pendingNotification(String code, String recipient, String subject) {
        Notification notification = new Notification();
        notification.setCode(code);
        notification.setChannel(NotificationChannel.EMAIL);
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setContent("内容-" + subject);
        notification.setStatus(NotificationStatus.PENDING);
        return notification;
    }
}
