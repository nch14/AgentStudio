package com.chenhaonee.agents.domain.agent.model;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.Date;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 云端备份文件记录。
 * 以本地文件系统为主存储，本实体仅在 OSS 备份场景下记录备份元数据。
 */
@Entity
@Data
@NoArgsConstructor
@Table(
        name = "cloud_file",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agent_file_agent_path_name", columnNames = {"agent_code", "path", "name"})
        }
)
@EqualsAndHashCode(callSuper = true)
public class CloudFile extends BaseEntity {

    @Column(name = "agent_code", length = 128, nullable = false, updatable = false)
    private String agentCode;

    /** 逻辑路径（目录），例如 persona/、memory/daily/ */
    @Column(nullable = false)
    private String path;

    /** 文件名，例如 core.md */
    @Column(nullable = false)
    private String name;

    /** 最近一次成功备份的 OSS 存储路径。nullable，备份成功后才写入。 */
    @Column(name = "oss_key", nullable = true)
    private String ossKey;

    /** 文件大小 */
    private Long fileSize;

    /** 最近一次备份时的文件 mtime，用于增量备份比对 */
    private Date lastSyncTime;

    /** 是否有效 */
    @Column(nullable = false)
    private boolean valid = true;

    public void initialize(String agentCode, String path, String name) {
        this.agentCode = ValidationUtils.requireText(agentCode, "agentCode");
        this.path = ValidationUtils.requireText(path, "path");
        this.name = ValidationUtils.requireText(name, "name");
        this.valid = true;
    }

    public void markInvalid() {
        this.valid = false;
    }
}
