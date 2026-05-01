package com.chenhaonee.agents.domain.agent.model;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.common.BaseEntity;
import com.chenhaonee.agents.domain.common.StringMapJsonConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.Map;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 系统内一个可被管理、可被执行的 Agent 实例。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "agent")
@EqualsAndHashCode(callSuper = true)
public class Agent extends BaseEntity {

    /** Agent 名字，面向用户展示。 */
    private String name;

    /** 面向用户展示的职责说明。 */
    private String responsibility;

    /** 技术实现链路标识，负责路由具体 agent-impl。 */
    @Enumerated(EnumType.STRING)
    private AgentProvider provider;

    /** 启停状态。 */
    @Enumerated(EnumType.STRING)
    private Status status;

    /** 实例级 provider 参数配置，JSON 存储，由具体 impl 自行解析。 */
    @Convert(converter = StringMapJsonConverter.class)
    @Column(columnDefinition = "TEXT")
    private Map<String, String> providerConfig;

    public void enable() {
        this.status = Status.ENABLED;
    }

    public void disable() {
        this.status = Status.DISABLED;
    }

    public boolean isEnabled() {
        return status == Status.ENABLED;
    }
}
