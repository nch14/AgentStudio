package com.chenhaonee.agents.domain.schedule.model;

import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * 带时间规则的自动化任务模板。
 * 每次命中时间规则后，系统创建新的 Task 和对应 TaskRun。
 */
@Entity
@Data
@NoArgsConstructor
@Table(name = "automation_plan")
@EqualsAndHashCode(callSuper = true)
public class AutomationPlan extends BaseEntity {

    private String name;

    private String agentCode;

    /** 自动化任务指令。 */
    private String instruction;

    /** 调度规则，cron 表达式。 */
    private String scheduleRule;

    /** 结果交付方式：PUSH 或 CONVERSATION。 */
    @Enumerated(EnumType.STRING)
    private DeliveryMode deliveryMode;

    @Enumerated(EnumType.STRING)
    private Status status;

    /** 上次触发时间，用于 cron 幂等判断。 */
    private Instant lastTriggeredAt;

    public void enable() {
        this.status = Status.ENABLED;
    }

    public void disable() {
        this.status = Status.DISABLED;
    }

    public boolean isEnabled() {
        return status == Status.ENABLED;
    }

    /**
     * 标记计划已被触发，更新 lastTriggeredAt 用于幂等控制。
     */
    public void trigger() {
        this.lastTriggeredAt = Instant.now();
    }

    /**
     * 结果交付方式。
     */
    public enum DeliveryMode {
        /** 一次性推送通知 */
        PUSH,
        /** 创建对话会话进行多轮交互 */
        CONVERSATION
    }
}
