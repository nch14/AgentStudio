package com.chenhaonee.agents.domain.agent.model;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.common.validator.ValidationUtils;
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
 * Skill 资产聚合根。
 * 表示一项可复用的能力资产，内容存储在 OSS 中。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "skill")
@EqualsAndHashCode(callSuper = true)
public class Skill extends BaseEntity {

    private String name;

    /** 用于管理列表展示的简短描述。 */
    @Column(length = 256)
    private String briefDescription;

    private String summary;

    @Enumerated(EnumType.STRING)
    private Status status;

    /** Skill 标题 */
    private String title;

    /** 当前内容的 OSS 存储路径 */
    @Column(name = "oss_key")
    private String ossKey;

    /** 文件大小 */
    private Long fileSize;

    public void enable() {
        this.status = Status.ENABLED;
    }

    public void disable() {
        this.status = Status.DISABLED;
    }

    public boolean isEnabled() {
        return status == Status.ENABLED;
    }

    public void updateContent(String ossKey, Long fileSize) {
        this.ossKey = ValidationUtils.requireText(ossKey, "ossKey");
        this.fileSize = fileSize;
    }
}
