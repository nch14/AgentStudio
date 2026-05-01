package com.chenhaonee.agents.app.application.scheduler;

import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.common.notify.Notifier;
import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 通知定时调度器。捞取待发送通知，按投递目标分组合并后发送。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyScheduler {

    private final MessageCenter messageCenter;

    @Value("${app.notify.scheduler.enabled:true}")
    private boolean enabled;

    @Scheduled(fixedRateString = "${app.notify.scheduler.fixed-rate:60000}")
    public void schedule() {
        if (!enabled) {
            return;
        }

        List<Notification> pending = messageCenter.getPendingNotifications();
        if (pending.isEmpty()) {
            return;
        }

        // 按渠道 + recipient 分组，避免不同接收人被错误合并发送
        Map<DeliveryTarget, List<Notification>> grouped = new HashMap<>();
        for (Notification n : pending) {
            DeliveryTarget target = new DeliveryTarget(n.getChannel(), n.getRecipient());
            grouped.computeIfAbsent(target, k -> new ArrayList<>()).add(n);
        }

        // 每个投递目标内合并发送
        int totalSent = 0;
        for (Map.Entry<DeliveryTarget, List<Notification>> entry : grouped.entrySet()) {
            totalSent += sendGrouped(entry.getKey().channel(), entry.getValue());
        }

        log.info("notify scheduler tick done, sent={}", totalSent);
    }

    /**
     * 发送同一投递目标的通知组。单条直接发送，多条合并为一条推送。
     */
    private int sendGrouped(NotificationChannel channel, List<Notification> notifications) {
        Notifier notifier = messageCenter.getNotifier(channel);
        if (notifier == null) {
            log.error("No notifier available for channel={}, registeredChannels={}",
                    channel, messageCenter.getRegisteredNotifierChannels());
            for (Notification n : notifications) {
                n.markFailed("no notifier for channel: " + channel);
            }
            messageCenter.saveAll(notifications);
            return 0;
        }

        if (notifications.size() == 1) {
            Notification n = notifications.get(0);
            try {
                notifier.send(n.getRecipient(), n.getSubject(), n.getContent());
                n.markSent();
            } catch (Exception e) {
                n.markFailed(e.getMessage());
            }
            messageCenter.saveAll(List.of(n));
            return 1;
        }

        // 合并：使用第一条的 recipient，subject 和 content 合并
        String recipient = notifications.get(0).getRecipient();
        String subject = notifications.size() + " 条通知";
        StringBuilder content = new StringBuilder();
        for (Notification n : notifications) {
            content.append(n.getSubject()).append("：").append(n.getContent()).append("\n");
        }

        try {
            log.info("Sending merged notification via notifier, channel={}, notifierType={}, size={}",
                    channel, notifier.getClass().getSimpleName(), notifications.size());
            notifier.send(recipient, subject, content.toString());
            for (Notification n : notifications) {
                n.markSent();
            }
        } catch (Exception e) {
            for (Notification n : notifications) {
                n.markFailed(e.getMessage());
            }
        }
        messageCenter.saveAll(notifications);
        return notifications.size();
    }

    private record DeliveryTarget(NotificationChannel channel, String recipient) {
    }
}
