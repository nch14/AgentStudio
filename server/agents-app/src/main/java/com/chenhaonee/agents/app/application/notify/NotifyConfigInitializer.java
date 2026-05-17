package com.chenhaonee.agents.app.application.notify;

import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * 启动时初始化默认通知配置。为每个内置事件创建一条默认 NotifyConfig 记录。
 */
@Component
public class NotifyConfigInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(NotifyConfigInitializer.class);

    private final NotifyConfigRepository notifyConfigRepository;
    private final NotifyConfigCommandApplicationService commandApplicationService;

    public NotifyConfigInitializer(NotifyConfigRepository notifyConfigRepository,
                                   NotifyConfigCommandApplicationService commandApplicationService) {
        this.notifyConfigRepository = notifyConfigRepository;
        this.commandApplicationService = commandApplicationService;
    }

    @Override
    public void run(ApplicationArguments args) {
        int created = 0;
        for (NotificationEvent event : NotificationEvent.values()) {
            if (notifyConfigRepository.findByEventAndValidTrue(event).isEmpty()) {
                commandApplicationService.createDefaultConfig(event);
                created++;
                log.info("Created default notify config for event: {}", event.code());
            }
        }
        if (created > 0) {
            log.info("Notify config initialization done, created {} default configs", created);
        } else {
            log.info("All notification configs already exist, skip initialization");
        }
    }
}
