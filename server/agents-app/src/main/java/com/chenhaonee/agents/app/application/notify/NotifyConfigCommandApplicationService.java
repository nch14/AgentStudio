package com.chenhaonee.agents.app.application.notify;

import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig.DeliveryMode;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 通知配置命令应用服务。
 */
@Service
@RequiredArgsConstructor
public class NotifyConfigCommandApplicationService {

    private final NotifyConfigRepository notifyConfigRepository;

    @Transactional(rollbackFor = Exception.class)
    public NotifyConfig createConfig(String name, DeliveryMode deliveryMode, String channels) {
        NotifyConfig config = new NotifyConfig();
        config.setName(name);
        config.setDeliveryMode(deliveryMode);
        config.setChannels(channels);
        return notifyConfigRepository.save(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public NotifyConfig updateConfig(String configCode, String name, DeliveryMode deliveryMode, String channels) {
        NotifyConfig config = notifyConfigRepository.findByCode(configCode)
                .orElseThrow(() -> new IllegalArgumentException("通知配置不存在: " + configCode));
        if (name != null) {
            config.setName(name);
        }
        if (deliveryMode != null) {
            config.setDeliveryMode(deliveryMode);
        }
        if (channels != null) {
            config.setChannels(channels);
        }
        return notifyConfigRepository.save(config);
    }

    @Transactional(rollbackFor = Exception.class)
    public void deleteConfig(String configCode) {
        NotifyConfig config = notifyConfigRepository.findByCode(configCode)
                .orElseThrow(() -> new IllegalArgumentException("通知配置不存在: " + configCode));
        config.setValid(false);
        notifyConfigRepository.save(config);
    }
}
