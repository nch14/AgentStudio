package com.chenhaonee.agents.app.application.notify;

import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.repository.NotifyConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

/**
 * 通知配置查询应用服务。
 */
@Service
@RequiredArgsConstructor
public class NotifyConfigQueryApplicationService {

    private final NotifyConfigRepository notifyConfigRepository;

    public Page<NotifyConfig> listConfigs(int page, int size) {
        return notifyConfigRepository.findAll(
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime")));
    }

    public NotifyConfig getConfig(String configCode) {
        return notifyConfigRepository.findByCode(configCode)
                .orElseThrow(() -> new IllegalArgumentException("通知配置不存在: " + configCode));
    }
}
