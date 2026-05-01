package com.chenhaonee.agents.domain.common;

import com.chenhaonee.agents.common.domain.Identity;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

/**
 * 领域实体公共字段。
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    protected Long id;

    @Column(length = 128, unique = true, nullable = false, updatable = false)
    protected String code;

    @Column(updatable = false)
    protected Date createTime;

    protected Date updateTime;

    protected boolean valid = true;

    @PrePersist
    protected void onCreate() {
        Date now = new Date();
        if (code == null || code.isBlank()) {
            code = Identity.newIdentity().value();
        }
        if (createTime == null) {
            createTime = now;
        }
        updateTime = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updateTime = new Date();
    }
}
