package com.chenhaonee.agents.app.application.notify;

import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig.DeliveryMode;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知事件配置命令应用服务。
 */
@Service
@RequiredArgsConstructor
public class NotifyConfigCommandApplicationService {

    private final NotifyConfigRepository notifyConfigRepository;

    @Transactional(rollbackFor = Exception.class)
    public NotifyConfig updateConfig(NotificationEvent event, Boolean enabled,
                                     DeliveryMode deliveryMode, List<String> channels) {
        NotifyConfig config = notifyConfigRepository.findByEventAndValidTrue(event)
                .orElseGet(() -> createDefaultConfig(event));
        config.updatePreferences(enabled, deliveryMode, normalizeChannels(channels));
        return notifyConfigRepository.save(config);
    }

    public DeliveryMode parseDeliveryMode(String deliveryMode) {
        if (deliveryMode == null || deliveryMode.isBlank()) {
            return null;
        }
        try {
            return DeliveryMode.valueOf(deliveryMode.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("未知通知投递模式: " + deliveryMode);
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public NotifyConfig createDefaultConfig(NotificationEvent event) {
        return notifyConfigRepository.save(newDefaultConfig(event));
    }

    private NotifyConfig newDefaultConfig(NotificationEvent event) {
        NotifyConfig config = new NotifyConfig();
        config.bindEvent(event);
        config.setEnabled(false);
        config.setDeliveryMode(DeliveryMode.MERGED);
        return config;
    }

    private String normalizeChannels(List<String> channels) {
        if (channels == null) {
            return null;
        }
        if (channels.isEmpty()) {
            return "";
        }

        LinkedHashSet<NotificationChannel> normalized = new LinkedHashSet<>();
        for (String channel : channels) {
            if (channel == null || channel.isBlank()) {
                throw new IllegalArgumentException("通知渠道不能为空");
            }
            try {
                normalized.add(NotificationChannel.valueOf(channel.trim()));
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("未知通知渠道: " + channel);
            }
        }
        return String.join(",", normalized.stream().map(NotificationChannel::name).toList());
    }
}
