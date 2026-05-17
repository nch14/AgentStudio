package com.chenhaonee.agents.app.application.notify;

import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.model.NotificationEventGroup;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig.DeliveryMode;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 通知事件配置查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class NotifyConfigQueryApplicationService {

    private final NotifyConfigRepository notifyConfigRepository;

    public List<NotifyConfigView> listConfigs() {
        Map<NotificationEvent, NotifyConfig> configs = loadConfiguredEvents();
        return Arrays.stream(NotificationEvent.values())
                .map(event -> toView(event, configs.get(event)))
                .toList();
    }

    public NotifyConfigView getConfig(NotificationEvent event) {
        NotifyConfig config = notifyConfigRepository.findByEventAndValidTrue(event).orElse(null);
        return toView(event, config);
    }

    private Map<NotificationEvent, NotifyConfig> loadConfiguredEvents() {
        EnumMap<NotificationEvent, NotifyConfig> configs = new EnumMap<>(NotificationEvent.class);
        for (NotifyConfig config : notifyConfigRepository.findByEventIsNotNullAndValidTrueOrderByUpdateTimeDesc()) {
            configs.putIfAbsent(config.getEvent(), config);
        }
        return configs;
    }

    private NotifyConfigView toView(NotificationEvent event, NotifyConfig config) {
        NotificationEventGroup group = event.group();
        return new NotifyConfigView(
                group.code(),
                group.displayName(),
                event.code(),
                event.displayName(),
                event.description(),
                config != null && config.enabled(),
                resolveDeliveryMode(config).name(),
                parseChannels(config),
                config != null,
                config != null && config.getCreateTime() != null ? config.getCreateTime().toString() : null,
                config != null && config.getUpdateTime() != null ? config.getUpdateTime().toString() : null);
    }

    private DeliveryMode resolveDeliveryMode(NotifyConfig config) {
        if (config == null || config.getDeliveryMode() == null) {
            return DeliveryMode.MERGED;
        }
        return config.getDeliveryMode();
    }

    private List<String> parseChannels(NotifyConfig config) {
        if (config == null || config.getChannels() == null || config.getChannels().isBlank()) {
            return List.of();
        }
        return Arrays.stream(config.getChannels().split(","))
                .map(String::trim)
                .filter(channel -> !channel.isEmpty())
                .toList();
    }
}
