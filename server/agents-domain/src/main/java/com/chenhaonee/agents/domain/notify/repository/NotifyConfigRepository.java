package com.chenhaonee.agents.domain.notify.repository;

import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 通知类型配置仓储。
 */
public interface NotifyConfigRepository extends JpaRepository<NotifyConfig, Long> {

    Optional<NotifyConfig> findByCode(String code);
}
