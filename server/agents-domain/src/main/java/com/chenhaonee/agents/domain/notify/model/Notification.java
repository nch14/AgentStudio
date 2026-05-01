package com.chenhaonee.agents.domain.notify.model;

import com.chenhaonee.agents.common.notify.NotificationChannel;
import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 通知消息记录。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "notification")
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

    /** 关联的通知类型配置编码 */
    @Column(length = 64)
    private String configCode;

    @Enumerated(EnumType.STRING)
    private NotificationChannel channel;

    private String recipient;

    private String subject;

    @Lob
    private String content;

    @Enumerated(EnumType.STRING)
    private NotificationStatus status;

    private String errorMessage;

    private Date sentAt;

    private Date readAt;

    public void markSent() {
        this.status = NotificationStatus.SENT;
        this.sentAt = new Date();
    }

    public void markFailed(String errorMessage) {
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public void markRead() {
        this.readAt = new Date();
    }

    public enum NotificationStatus {
        /** 待发送 */
        PENDING,
        /** 已发送 */
        SENT,
        /** 发送失败 */
        FAILED
    }
}
