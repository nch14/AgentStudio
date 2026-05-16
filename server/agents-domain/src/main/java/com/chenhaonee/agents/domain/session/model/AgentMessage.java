package com.chenhaonee.agents.domain.session.model;

import com.chenhaonee.agents.domain.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Agent 会话中的一条消息。
 * 每条记录对应一个用户可见的消息 block。
 */
@Entity
@Data
@NoArgsConstructor
@Table(
        name = "agent_message",
        indexes = {
                @Index(name = "idx_agent_message_session_turn", columnList = "session_code,turn_code"),
                @Index(name = "idx_agent_message_turn", columnList = "turn_code")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_agent_message_session_index", columnNames = {"session_code", "message_index"})
        }
)
@EqualsAndHashCode(callSuper = true)
public class AgentMessage extends BaseEntity {

    public static AgentMessage block(
            String sessionCode,
            String turnCode,
            MessageRole role,
            ContentBlockType blockType,
            MessageProtocolType protocolType,
            String payloadJson,
            String externalMessageId
    ) {
        validateRoleAndBlockType(role, blockType);
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setTurnCode(turnCode);
        message.setRole(role);
        message.setContentBlockType(blockType);
        message.setProtocolType(protocolType);
        message.setStatus(MessageStatus.COMPLETED);
        message.setPayloadJson(payloadJson);
        message.setExternalMessageId(externalMessageId);
        return message;
    }

    public static AgentMessage blockCancelled(
            String sessionCode,
            String turnCode,
            MessageRole role,
            ContentBlockType blockType,
            MessageProtocolType protocolType,
            String payloadJson,
            String externalMessageId
    ) {
        AgentMessage message = block(sessionCode, turnCode, role, blockType, protocolType, payloadJson, externalMessageId);
        message.setStatus(MessageStatus.CANCELLED);
        return message;
    }

    /**
     * @deprecated 旧 Anthropic 整段消息桥接，等待第三批 recorder 接管后移除。
     */
    @Deprecated
    public static AgentMessage anthropicUserMessage(String sessionCode, String payloadJson) {
        return legacyMessage(sessionCode, MessageRole.USER, MessageProtocolType.ANTHROPIC_MESSAGES, payloadJson, MessageStatus.COMPLETED);
    }

    /**
     * @deprecated 旧 Anthropic placeholder 桥接，等待第三批 recorder 接管后移除。
     */
    @Deprecated
    public static AgentMessage anthropicAssistantPlaceholder(String sessionCode) {
        return legacyMessage(sessionCode, MessageRole.ASSISTANT, MessageProtocolType.ANTHROPIC_MESSAGES, null, MessageStatus.STREAMING);
    }

    private static AgentMessage legacyMessage(
            String sessionCode,
            MessageRole role,
            MessageProtocolType protocolType,
            String payloadJson,
            MessageStatus status
    ) {
        AgentMessage message = new AgentMessage();
        message.setSessionCode(sessionCode);
        message.setTurnCode(sessionCode);
        message.setRole(role);
        message.setContentBlockType(ContentBlockType.TEXT);
        message.setProtocolType(protocolType);
        message.setStatus(status);
        message.setPayloadJson(payloadJson);
        return message;
    }

    @Column(name = "session_code", length = 128, nullable = false, updatable = false)
    private String sessionCode;

    @Column(name = "turn_code", length = 64, nullable = false, updatable = false)
    private String turnCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MessageRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_block_type", nullable = false, length = 32, updatable = false)
    private ContentBlockType contentBlockType = ContentBlockType.TEXT;

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

    /**
     * @deprecated 旧流式生命周期方法，等待第三批 recorder 接管后移除。
     */
    @Deprecated
    public void complete(String payloadJson, String externalMessageId) {
        this.status = MessageStatus.COMPLETED;
        this.payloadJson = payloadJson;
        this.externalMessageId = externalMessageId;
        this.errorPayloadJson = null;
    }

    /**
     * @deprecated 旧流式生命周期方法，等待第三批 recorder 接管后移除。
     */
    @Deprecated
    public void fail(String partialPayloadJson, String errorPayloadJson) {
        this.status = MessageStatus.FAILED;
        this.payloadJson = partialPayloadJson;
        this.errorPayloadJson = errorPayloadJson;
    }

    public String getProtocolTypeCode() {
        return protocolType == null ? null : protocolType.name();
    }

    public String getStatusCode() {
        return status == null ? null : status.name();
    }

    private static void validateRoleAndBlockType(MessageRole role, ContentBlockType blockType) {
        if (role == null) {
            throw new IllegalArgumentException("role cannot be null");
        }
        if (blockType == null) {
            throw new IllegalArgumentException("contentBlockType cannot be null");
        }
        boolean valid = switch (role) {
            case USER -> blockType == ContentBlockType.TEXT
                    || blockType == ContentBlockType.IMAGE
                    || blockType == ContentBlockType.DOCUMENT;
            case ASSISTANT -> blockType == ContentBlockType.TEXT
                    || blockType == ContentBlockType.THINKING
                    || blockType == ContentBlockType.TOOL_USE
                    || blockType == ContentBlockType.IMAGE
                    || blockType == ContentBlockType.DOCUMENT
                    || blockType == ContentBlockType.TURN_START
                    || blockType == ContentBlockType.TURN_STOP;
            case TOOL -> blockType == ContentBlockType.TOOL_RESULT;
            case SYSTEM -> blockType == ContentBlockType.TEXT;
        };
        if (!valid) {
            throw new IllegalArgumentException("invalid role/contentBlockType combination: " + role + "/" + blockType);
        }
    }
}
