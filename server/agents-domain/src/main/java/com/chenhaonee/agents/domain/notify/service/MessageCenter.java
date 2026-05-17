package com.chenhaonee.agents.domain.notify.service;

import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.common.notify.Notifier;
import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.model.Notification.NotificationStatus;
import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.model.NotificationTemplate;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig.DeliveryMode;
import com.chenhaonee.agents.domain.notify.repository.NotificationRepository;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import com.chenhaonee.agents.domain.profile.service.OwnerProfileDomainService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 消息中心领域服务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MessageCenter {

    private final NotificationRepository notificationRepository;
    private final NotifyConfigRepository notifyConfigRepository;
    private final List<Notifier> notifierList;
    private final OwnerProfileDomainService ownerProfileDomainService;
    private Map<NotificationChannel, Notifier> notifiers;

    @PostConstruct
    void initNotifiers() {
        EnumMap<NotificationChannel, Notifier> registry = new EnumMap<>(NotificationChannel.class);
        for (Notifier notifier : notifierList) {
            NotificationChannel channel = notifier.getChannel();
            if (channel == null) {
                throw new IllegalStateException("notifier channel must not be null: " + notifier.getClass().getName());
            }
            Notifier previous = registry.putIfAbsent(channel, notifier);
            if (previous != null) {
                throw new IllegalStateException("duplicate notifier for channel: " + channel);
            }
        }
        this.notifiers = Map.copyOf(registry);
        log.info("Initialized notifiers, channels={}", this.notifiers.keySet());
    }

    /**
     * 发送通知。根据 configCode 查找 NotifyConfig，自动解析渠道和接收人。
     * INSTANT 模式立即发送；MERGED 模式创建 PENDING 记录，由 NotifyScheduler 捞取发送。
     */
    @Transactional(rollbackFor = Exception.class)
    public void send(String configCode, String subject, String content) {
        if (configCode == null || configCode.isBlank()) {
            log.warn("Skip sending notification because configCode is blank, subject={}", subject);
            return;
        }

        NotifyConfig config = notifyConfigRepository.findByCode(configCode).orElse(null);
        if (config == null) {
            log.warn("Skip sending notification because notify config was not found, configCode={}, subject={}",
                    configCode, subject);
            return;
        }

        OwnerProfile profile = ownerProfileDomainService.requireCurrent();
        List<NotificationChannel> channels = resolveChannels(config, profile);

        if (channels.isEmpty()) {
            return;
        }

        if (config.getDeliveryMode() == DeliveryMode.INSTANT) {
            sendImmediately(channels, profile, subject, content, configCode);
        } else {
            enqueue(channels, profile, subject, content, configCode);
        }
    }

    /**
     * 发送通知（事件化入口）。根据通知事件查找 NotifyConfig，自动解析渠道和接收人。
     * INSTANT 模式立即发送；MERGED 模式创建 PENDING 记录，由 NotifyScheduler 捞取发送。
     */
    @Transactional(rollbackFor = Exception.class)
    public void send(NotificationEvent event, Map<String, String> variables) {
        if (event == null) {
            throw new IllegalArgumentException("通知事件不能为空");
        }
        Map<String, String> renderVariables = variables == null ? Map.of() : variables;

        NotifyConfig config = notifyConfigRepository.findByEventAndValidTrue(event).orElse(null);
        if (config == null) {
            log.info("Skip sending notification because notify config was not found, eventCode={}", event.code());
            return;
        }
        if (!config.enabled()) {
            log.info("Skip sending notification because notify config is disabled, eventCode={}", event.code());
            return;
        }

        OwnerProfile profile = ownerProfileDomainService.requireCurrent();
        List<NotificationChannel> channels = resolveChannels(config, profile);

        if (channels.isEmpty()) {
            return;
        }

        DeliveryMode deliveryMode = config.getDeliveryMode() == null ? DeliveryMode.MERGED : config.getDeliveryMode();
        if (deliveryMode == DeliveryMode.INSTANT) {
            sendImmediately(event, channels, profile, renderVariables);
        } else {
            enqueue(event, channels, profile, renderVariables);
        }
    }

    /**
     * 立即发送模式（事件化）：对每个配置的渠道直接发送通知。
     */
    private void sendImmediately(NotificationEvent event, List<NotificationChannel> channels,
                                 OwnerProfile profile, Map<String, String> variables) {
        for (NotificationChannel channel : channels) {
            String recipient = resolveRecipient(channel, profile);
            if (recipient == null) {
                continue;
            }
            NotificationTemplate.RenderedNotification notification =
                    NotificationTemplate.render(event, channel, variables);
            doSendEvent(channel, recipient, notification.subject(), notification.content(), event.code());
        }
    }

    /**
     * 合并模式（事件化）：创建 PENDING 记录，等 NotifyScheduler 捞取发送。
     */
    private void enqueue(NotificationEvent event, List<NotificationChannel> channels,
                         OwnerProfile profile, Map<String, String> variables) {
        for (NotificationChannel channel : channels) {
            String recipient = resolveRecipient(channel, profile);
            if (recipient == null) {
                continue;
            }
            NotificationTemplate.RenderedNotification renderedNotification =
                    NotificationTemplate.render(event, channel, variables);
            Notification notification = new Notification();
            notification.setEventCode(event.code());
            notification.setChannel(channel);
            notification.setRecipient(recipient);
            notification.setSubject(renderedNotification.subject());
            notification.setContent(renderedNotification.content());
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        }
    }

    /**
     * 立即发送模式：对每个配置的渠道直接发送通知。
     */
    private void sendImmediately(List<NotificationChannel> channels, OwnerProfile profile,
                                 String subject, String content, String configCode) {
        for (NotificationChannel channel : channels) {
            String recipient = resolveRecipient(channel, profile);
            if (recipient == null) {
                continue;
            }
            doSend(channel, recipient, subject, content, configCode);
        }
    }

    /**
     * 合并模式：创建 PENDING 记录，等 NotifyScheduler 捞取。
     */
    private void enqueue(List<NotificationChannel> channels, OwnerProfile profile,
                         String subject, String content, String configCode) {
        for (NotificationChannel channel : channels) {
            String recipient = resolveRecipient(channel, profile);
            if (recipient == null) {
                continue;
            }
            Notification notification = new Notification();
            notification.setConfigCode(configCode);
            notification.setChannel(channel);
            notification.setRecipient(recipient);
            notification.setSubject(subject);
            notification.setContent(content);
            notification.setStatus(NotificationStatus.PENDING);
            notificationRepository.save(notification);
        }
    }

    /**
     * 执行单条通知发送。
     */
    @Transactional(rollbackFor = Exception.class)
    public Notification doSend(NotificationChannel channel, String recipient,
                               String subject, String content, String configCode) {
        Notification notification = new Notification();
        notification.setConfigCode(configCode);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);

        Notifier notifier = getNotifier(channel);
        if (notifier == null) {
            log.error("No notifier available for channel={}, registeredChannels={}",
                    channel, getRegisteredNotifierChannels());
            notification.markFailed("no notifier available for channel: " + channel);
            return notificationRepository.save(notification);
        }

        try {
            log.info("Sending notification via notifier, channel={}, notifierType={}, configCode={}",
                    channel, notifier.getClass().getSimpleName(), configCode);
            notifier.send(recipient, subject, content);
            notification.markSent();
        } catch (Exception e) {
            notification.markFailed(e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    public Notifier getNotifier(NotificationChannel channel) {
        return notifiers.get(channel);
    }

    public Set<NotificationChannel> getRegisteredNotifierChannels() {
        return notifiers.keySet();
    }

    /**
     * 解析通知渠道列表。优先使用配置中的 channels，为空时自动选择 OwnerProfile 中可用的渠道。
     */
    private List<NotificationChannel> resolveChannels(NotifyConfig config, OwnerProfile profile) {
        if (config.getChannels() != null && !config.getChannels().isBlank()) {
            return Arrays.stream(config.getChannels().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(this::parseChannel)
                    .filter(channel -> channel != null)
                    .toList();
        }

        // 自动选择
        List<NotificationChannel> channels = new ArrayList<>();
        if (profile.getBarkDeviceKey() != null && !profile.getBarkDeviceKey().isBlank()) {
            channels.add(NotificationChannel.BARK);
        }
        if (profile.getEmail() != null && !profile.getEmail().isBlank()) {
            channels.add(NotificationChannel.EMAIL);
        }
        return channels;
    }

    private NotificationChannel parseChannel(String channelName) {
        try {
            return NotificationChannel.valueOf(channelName);
        } catch (IllegalArgumentException e) {
            log.warn("Skip invalid notification channel in config, channel={}", channelName);
            return null;
        }
    }

    private Notification doSendEvent(NotificationChannel channel, String recipient,
                                     String subject, String content, String eventCode) {
        Notification notification = new Notification();
        notification.setEventCode(eventCode);
        notification.setChannel(channel);
        notification.setRecipient(recipient);
        notification.setSubject(subject);
        notification.setContent(content);
        notification.setStatus(NotificationStatus.PENDING);
        notification = notificationRepository.save(notification);

        Notifier notifier = getNotifier(channel);
        if (notifier == null) {
            log.error("No notifier available for channel={}, registeredChannels={}",
                    channel, getRegisteredNotifierChannels());
            notification.markFailed("no notifier available for channel: " + channel);
            return notificationRepository.save(notification);
        }

        try {
            log.info("Sending notification via notifier, channel={}, notifierType={}, eventCode={}",
                    channel, notifier.getClass().getSimpleName(), eventCode);
            notifier.send(recipient, subject, content);
            notification.markSent();
        } catch (Exception e) {
            notification.markFailed(e.getMessage());
        }

        return notificationRepository.save(notification);
    }

    /**
     * 从 OwnerProfile 中获取对应渠道的接收人地址。
     */
    private String resolveRecipient(NotificationChannel channel, OwnerProfile profile) {
        return switch (channel) {
            case BARK -> profile.getBarkDeviceKey();
            case EMAIL -> profile.getEmail();
        };
    }

    /**
     * 分页查询通知记录。
     */
    public Page<Notification> getNotifications(int page, int size) {
        return notificationRepository.findByValidTrueOrderByCreateTimeDesc(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    /**
     * 未读消息数量。
     */
    public long getUnreadCount() {
        return notificationRepository.countByValidTrueAndStatusAndReadAtIsNull(NotificationStatus.SENT);
    }

    /**
     * 标记通知已读。
     */
    @Transactional(rollbackFor = Exception.class)
    public void markRead(String code) {
        Notification notification = notificationRepository.findByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("notification not found: " + code));
        notification.markRead();
        notificationRepository.save(notification);
    }

    /**
     * 查询所有待发送的通知。
     */
    public List<Notification> getPendingNotifications() {
        return notificationRepository.findByStatusAndValidTrueOrderByCreateTimeAsc(NotificationStatus.PENDING);
    }

    /**
     * 批量保存通知状态变更。
     */
    @Transactional(rollbackFor = Exception.class)
    public void saveAll(List<Notification> notifications) {
        notificationRepository.saveAll(notifications);
    }
}
