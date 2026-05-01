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
 * 通知类型配置。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "notify_config")
@EqualsAndHashCode(callSuper = true)
public class NotifyConfig extends BaseEntity {

    /** 展示名称 */
    private String name;

    /** 投递模式：INSTANT 立即发送，MERGED 合并定时发送 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryMode deliveryMode;

    /** 启用的通知渠道，逗号分隔如 "BARK,EMAIL" */
    private String channels;

    public enum DeliveryMode {
        /** 立即发送 */
        INSTANT,
        /** 合并定时发送 */
        MERGED
    }
}
