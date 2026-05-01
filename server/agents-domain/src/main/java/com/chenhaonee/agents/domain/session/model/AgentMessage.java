package com.chenhaonee.agents.domain.session.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 会话中的一条消息。
 * payloadJson 根据不同协议存储标准消息 JSON。
 */
@Entity
@Data
@NoArgsConstructor
@Table(
        name = "agent_message",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agent_message_session_index", columnNames = {"session_code", "message_index"})
        }
)
@EqualsAndHashCode(callSuper = true)
public class AgentMessage extends BaseEntity {

    public static AgentMessage openAiUserMessage(String sessionCode, String payloadJson) {
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setRole(MessageRole.USER);
        message.setProtocolType(MessageProtocolType.OPENAI_CHAT_COMPLETIONS);
        message.setStatus(MessageStatus.COMPLETED);
        message.setPayloadJson(payloadJson);
        return message;
    }

    public static AgentMessage openAiAssistantPlaceholder(String sessionCode) {
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setRole(MessageRole.ASSISTANT);
        message.setProtocolType(MessageProtocolType.OPENAI_CHAT_COMPLETIONS);
        message.setStatus(MessageStatus.STREAMING);
        return message;
    }

    public static AgentMessage anthropicUserMessage(String sessionCode, String payloadJson) {
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setRole(MessageRole.USER);
        message.setProtocolType(MessageProtocolType.ANTHROPIC_MESSAGES);
        message.setStatus(MessageStatus.COMPLETED);
        message.setPayloadJson(payloadJson);
        return message;
    }

    public static AgentMessage anthropicAssistantPlaceholder(String sessionCode) {
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setRole(MessageRole.ASSISTANT);
        message.setProtocolType(MessageProtocolType.ANTHROPIC_MESSAGES);
        message.setStatus(MessageStatus.STREAMING);
        return message;
    }

    @Column(name = "session_code", length = 128, nullable = false, updatable = false)
    private String sessionCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "protocol_type", length = 64)
    private MessageProtocolType protocolType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private MessageStatus status = MessageStatus.COMPLETED;

    @Column(name = "payload_json", columnDefinition = "MEDIUMTEXT")
    private String payloadJson;

    @Column(name = "error_payload_json", columnDefinition = "MEDIUMTEXT")
    private String errorPayloadJson;

    @Column(name = "external_message_id", length = 128)
    private String externalMessageId;

    @Column(name = "message_index", nullable = false, updatable = false)
    private int messageIndex;

    public void assignMessageIndex(int messageIndex) {
        if (messageIndex <= 0) {
            throw new IllegalArgumentException("messageIndex must be positive");
        }
        this.messageIndex = messageIndex;
    }

    public void markStreaming(String initialPayloadJson) {
        this.status = MessageStatus.STREAMING;
        this.payloadJson = initialPayloadJson;
        this.errorPayloadJson = null;
    }

    public void complete(String payloadJson, String externalMessageId) {
        this.status = MessageStatus.COMPLETED;
        this.payloadJson = payloadJson;
        this.externalMessageId = externalMessageId;
        this.errorPayloadJson = null;
    }

    public void fail(String partialPayloadJson, String errorPayloadJson) {
        this.status = MessageStatus.FAILED;
        this.payloadJson = partialPayloadJson;
        this.errorPayloadJson = errorPayloadJson;
    }

    public void cancel(String partialPayloadJson, String errorPayloadJson) {
        this.status = MessageStatus.CANCELLED;
        this.payloadJson = partialPayloadJson;
        this.errorPayloadJson = errorPayloadJson;
    }

    public String getProtocolTypeCode() {
        return protocolType == null ? null : protocolType.name();
    }

    public String getStatusCode() {
        return status == null ? null : status.name();
    }

    /**
     * 兼容旧代码读取 content 字段，后续统一迁移到 payloadJson。
     */
    @Deprecated
    public String getContent() {
        return payloadJson;
    }

    /**
     * 兼容旧代码写入 content 字段，后续统一迁移到 payloadJson。
     */
    @Deprecated
    public void setContent(String content) {
        this.payloadJson = content;
    }
}
