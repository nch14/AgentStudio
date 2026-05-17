package com.chenhaonee.agents.domain.notify.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 通知事件配置。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "notify_config")
@EqualsAndHashCode(callSuper = true)
public class NotifyConfig extends BaseEntity {

    /** 系统内置通知事件 */
    @Enumerated(EnumType.STRING)
    @Column(length = 64)
    private NotificationEvent event;

    /** 展示名称 */
    private String name;

    /** 是否启用该事件通知 */
    private Boolean enabled;

    /** 投递模式：INSTANT 立即发送，MERGED 合并定时发送 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryMode deliveryMode;

    /** 启用的通知渠道，逗号分隔如 "BARK,EMAIL" */
    private String channels;

    /**
     * 将配置绑定到系统通知事件。
     */
    public void bindEvent(NotificationEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("通知事件不能为空");
        }
        this.event = event;
        if (this.name == null || this.name.isBlank()) {
            this.name = event.displayName();
        }
    }

    /**
     * 更新通知偏好。
     */
    public void updatePreferences(Boolean enabled, DeliveryMode deliveryMode, String channels) {
        if (enabled != null) {
            this.enabled = enabled;
        }
        if (deliveryMode != null) {
            this.deliveryMode = deliveryMode;
        }
        if (channels != null) {
            this.channels = channels.isBlank() ? null : channels;
        }
    }

    /**
     * 是否启用该事件通知。历史数据中 null 按 false 处理。
     */
    public boolean enabled() {
        return Boolean.TRUE.equals(enabled);
    }

    public enum DeliveryMode {
        /** 立即发送 */
        INSTANT,
        /** 合并定时发送 */
        MERGED
    }
}
