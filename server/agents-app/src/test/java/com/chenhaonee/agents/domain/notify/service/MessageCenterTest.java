package com.chenhaonee.agents.domain.notify.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.common.notify.Notifier;
import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.repository.NotificationRepository;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import com.chenhaonee.agents.domain.profile.service.OwnerProfileDomainService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessageCenterTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotifyConfigRepository notifyConfigRepository;

    @Mock
    private OwnerProfileDomainService ownerProfileDomainService;

    @Test
    void shouldSkipSendWhenConfigCodeIsBlank() {
        MessageCenter messageCenter = messageCenter(List.of());

        messageCenter.send("  ", "subject", "content");

        verify(notifyConfigRepository, never()).findByCode(any());
        verify(ownerProfileDomainService, never()).requireCurrent();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldSkipSendWhenNotifyConfigDoesNotExist() {
        MessageCenter messageCenter = messageCenter(List.of());
        when(notifyConfigRepository.findByCode("missing")).thenReturn(Optional.empty());

        messageCenter.send("missing", "subject", "content");

        verify(notifyConfigRepository).findByCode("missing");
        verify(ownerProfileDomainService, never()).requireCurrent();
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void shouldMarkNotificationReadByBusinessCode() {
        Notification notification = new Notification();
        notification.setCode("notice-1");
        MessageCenter messageCenter = messageCenter(List.of());

        when(notificationRepository.findByCode("notice-1")).thenReturn(Optional.of(notification));

        messageCenter.markRead("notice-1");

        assertNotNull(notification.getReadAt());
        verify(notificationRepository).findByCode("notice-1");
        verify(notificationRepository, never()).findById(any());
        verify(notificationRepository).save(notification);
    }

    @Test
    void shouldRegisterNotifiersByChannel() {
        Notifier barkNotifier = notifier(NotificationChannel.BARK);
        Notifier emailNotifier = notifier(NotificationChannel.EMAIL);

        MessageCenter messageCenter = messageCenter(List.of(barkNotifier, emailNotifier));

        assertSame(barkNotifier, messageCenter.getNotifier(NotificationChannel.BARK));
        assertSame(emailNotifier, messageCenter.getNotifier(NotificationChannel.EMAIL));
    }

    @Test
    void shouldRejectDuplicateNotifierChannel() {
        Notifier firstBarkNotifier = notifier(NotificationChannel.BARK);
        Notifier secondBarkNotifier = notifier(NotificationChannel.BARK);
        MessageCenter messageCenter = new MessageCenter(
                notificationRepository,
                notifyConfigRepository,
                List.of(firstBarkNotifier, secondBarkNotifier),
                ownerProfileDomainService);

        assertThrows(IllegalStateException.class, messageCenter::initNotifiers);
    }

    @Test
    void shouldRejectNotifierWithoutChannel() {
        MessageCenter messageCenter = new MessageCenter(
                notificationRepository,
                notifyConfigRepository,
                List.of(notifier(null)),
                ownerProfileDomainService);

        assertThrows(IllegalStateException.class, messageCenter::initNotifiers);
    }

    private MessageCenter messageCenter(List<Notifier> notifiers) {
        MessageCenter messageCenter = new MessageCenter(
                notificationRepository,
                notifyConfigRepository,
                notifiers,
                ownerProfileDomainService);
        messageCenter.initNotifiers();
        return messageCenter;
    }

    private Notifier notifier(NotificationChannel channel) {
        return new Notifier() {
            @Override
            public NotificationChannel getChannel() {
                return channel;
            }

            @Override
            public void send(String recipient, String subject, String content) {
            }
        };
    }
}
