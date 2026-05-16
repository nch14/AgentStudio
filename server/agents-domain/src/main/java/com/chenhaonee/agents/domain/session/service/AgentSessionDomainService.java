package com.chenhaonee.agents.domain.session.service;

import com.chenhaonee.agents.domain.session.model.AgentMessage;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.AgentSummary;
import com.chenhaonee.agents.domain.session.model.ContentBlockType;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.MessageRole;
import com.chenhaonee.agents.domain.session.model.MessageStatus;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import com.chenhaonee.agents.domain.session.repository.AgentMessageRepository;
import com.chenhaonee.agents.domain.session.repository.AgentSessionRepository;
import com.chenhaonee.agents.domain.session.repository.AgentSessionSummaryRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 会话领域服务。
 */
@Service
public class AgentSessionDomainService {

    private static final int SNIPPET_MAX_LENGTH = 100;

    private final AgentMessageRepository agentMessageRepository;
    private final AgentSessionRepository agentSessionRepository;
    private final AgentSessionSummaryRepository agentSessionSummaryRepository;

    public AgentSessionDomainService(
            AgentMessageRepository agentMessageRepository,
            AgentSessionRepository agentSessionRepository,
            AgentSessionSummaryRepository agentSessionSummaryRepository
    ) {
        this.agentMessageRepository = agentMessageRepository;
        this.agentSessionRepository = agentSessionRepository;
        this.agentSessionSummaryRepository = agentSessionSummaryRepository;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentSession appendMessage(String sessionCode, AgentMessage message) {
        return appendMessageInternal(sessionCode, message);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentMessage appendMessageAndReturnMessage(String sessionCode, AgentMessage message) {
        appendMessageInternal(sessionCode, message);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentMessage appendBlock(
            String sessionCode,
            String turnCode,
            MessageRole role,
            ContentBlockType blockType,
            MessageProtocolType protocolType,
            String payloadJson,
            String externalMessageId
    ) {
        AgentMessage message = AgentMessage.block(
                sessionCode,
                turnCode,
                role,
                blockType,
                protocolType,
                payloadJson,
                externalMessageId
        );
        appendMessageInternal(sessionCode, message);
        return message;
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentMessage appendCancelledBlock(
            String sessionCode,
            String turnCode,
            MessageRole role,
            ContentBlockType blockType,
            MessageProtocolType protocolType,
            String payloadJson,
            String externalMessageId
    ) {
        AgentMessage message = AgentMessage.blockCancelled(
                sessionCode,
                turnCode,
                role,
                blockType,
                protocolType,
                payloadJson,
                externalMessageId
        );
        appendMessageInternal(sessionCode, message);
        return message;
    }

    /**
     * @deprecated 旧流式生命周期方法，等待第三批 recorder 接管后移除。
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public AgentMessage completeMessage(String messageCode, String payloadJson, String externalMessageId) {
        AgentMessage message = getMessageByCode(messageCode);
        message.complete(payloadJson, externalMessageId);
        return agentMessageRepository.save(message);
    }

    /**
     * @deprecated 旧流式生命周期方法，等待第三批 recorder 接管后移除。
     */
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public AgentMessage failMessage(String messageCode, String partialPayloadJson, String errorPayloadJson) {
        AgentMessage message = getMessageByCode(messageCode);
        message.fail(partialPayloadJson, errorPayloadJson);
        return agentMessageRepository.save(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentSession rename(String sessionCode, String title) {
        AgentSession session = agentSessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionCode));
        session.rename(title);
        return agentSessionRepository.save(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentSession rename(String agentCode, String sessionCode, String title) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        return rename(sessionCode, title);
    }

    public List<AgentMessage> listMessages(String sessionCode) {
        if (!agentSessionRepository.existsByCode(sessionCode)) {
            throw new IllegalArgumentException("session not found: " + sessionCode);
        }
        return agentMessageRepository.findBySessionCodeOrderByMessageIndexAsc(sessionCode);
    }

    public Page<AgentMessage> listMessages(String sessionCode, Pageable pageable) {
        if (!agentSessionRepository.existsByCode(sessionCode)) {
            throw new IllegalArgumentException("session not found: " + sessionCode);
        }
        return agentMessageRepository.findBySessionCodeOrderByMessageIndexAsc(sessionCode, pageable);
    }

    public Page<AgentMessage> listMessages(String agentCode, String sessionCode, Pageable pageable) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        return listMessages(sessionCode, pageable);
    }

    public Page<MessageTurn> listMessageTurns(String agentCode, String sessionCode, Pageable pageable) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        Page<String> turnCodes = agentMessageRepository.findTurnCodesBySessionCodeOrderByFirstMessageIndex(sessionCode, pageable);
        if (turnCodes.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, turnCodes.getTotalElements());
        }
        List<AgentMessage> messages = agentMessageRepository.findBySessionCodeAndTurnCodeInOrderByMessageIndexAsc(
                sessionCode,
                turnCodes.getContent()
        );
        Map<String, List<AgentMessage>> grouped = new LinkedHashMap<>();
        for (String turnCode : turnCodes.getContent()) {
            grouped.put(turnCode, new ArrayList<>());
        }
        for (AgentMessage message : messages) {
            grouped.computeIfAbsent(message.getTurnCode(), ignored -> new ArrayList<>()).add(message);
        }
        List<MessageTurn> turns = turnCodes.getContent().stream()
                .map(turnCode -> new MessageTurn(turnCode, List.copyOf(grouped.getOrDefault(turnCode, List.of()))))
                .toList();
        return new PageImpl<>(turns, pageable, turnCodes.getTotalElements());
    }

    /**
     * 游标分页查询消息 turn 列表。
     * 不传 cursor 时返回最新的 size 个 turn；传 cursor 时返回首条 messageIndex < cursor 的 turn。
     * 游标以 turn 为粒度，确保分页边界不会把同一个 turn 拆散。
     */
    public CursorResult<MessageTurn> listMessageTurnsByCursor(
            String agentCode, String sessionCode, Integer cursor, int size) {
        getSessionByAgentAndCode(agentCode, sessionCode);

        Pageable limit = PageRequest.of(0, size + 1);
        List<String> turnCodesDesc;
        if (cursor == null) {
            turnCodesDesc = agentMessageRepository.findLatestTurnCodesBySessionCodeDesc(sessionCode, limit);
        } else {
            turnCodesDesc = agentMessageRepository.findTurnCodesBySessionCodeBeforeCursorDesc(
                    sessionCode, cursor, limit);
        }

        boolean hasMore = turnCodesDesc.size() > size;
        List<String> pageTurnCodes = new ArrayList<>(
                hasMore ? turnCodesDesc.subList(0, size) : turnCodesDesc);
        Collections.reverse(pageTurnCodes);

        if (pageTurnCodes.isEmpty()) {
            return new CursorResult<>(List.of(), null, false);
        }

        List<AgentMessage> messages = agentMessageRepository
                .findBySessionCodeAndTurnCodeInOrderByMessageIndexAsc(sessionCode, pageTurnCodes);

        Map<String, List<AgentMessage>> grouped = new LinkedHashMap<>();
        for (String turnCode : pageTurnCodes) {
            grouped.put(turnCode, new ArrayList<>());
        }
        for (AgentMessage msg : messages) {
            grouped.computeIfAbsent(msg.getTurnCode(), k -> new ArrayList<>()).add(msg);
        }

        List<MessageTurn> turns = pageTurnCodes.stream()
                .map(turnCode -> new MessageTurn(turnCode, List.copyOf(grouped.getOrDefault(turnCode, List.of()))))
                .toList();

        String nextCursor = null;
        if (hasMore) {
            MessageTurn oldestTurn = turns.get(0);
            int minIndex = oldestTurn.blocks().stream()
                    .mapToInt(AgentMessage::getMessageIndex)
                    .min()
                    .orElse(0);
            nextCursor = String.valueOf(minIndex);
        }

        return new CursorResult<>(turns, nextCursor, hasMore);
    }

    /**
     * 获取指定 session 中 messageIndex >= fromIndex 的所有消息。
     */
    public List<AgentMessage> listMessagesFromIndex(String agentCode, String sessionCode, int fromIndex) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        return agentMessageRepository.findBySessionCodeAndMessageIndexGreaterThanEqualOrderByMessageIndexAsc(
                sessionCode, fromIndex);
    }

    public AgentMessage getMessageByCode(String messageCode) {
        return agentMessageRepository.findByCode(messageCode)
                .orElseThrow(() -> new NoSuchElementException("message not found: " + messageCode));
    }

    public Page<AgentSession> listSessions(int page, int size, boolean archived, SessionScene scene) {
        PageRequest pageable = PageRequest.of(page, size);
        if (archived) {
            return agentSessionRepository.findBySceneAndArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(scene, pageable);
        }
        return agentSessionRepository.findBySceneAndArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(scene, pageable);
    }

    public Page<AgentSession> listSessionsByAgent(int page, int size, String agentCode, boolean archived, SessionScene scene) {
        PageRequest pageable = PageRequest.of(page, size);
        if (archived) {
            return agentSessionRepository.findByAgentCodeAndSceneAndArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(agentCode, scene, pageable);
        }
        return agentSessionRepository.findByAgentCodeAndSceneAndArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(agentCode, scene, pageable);
    }

    public AgentSession getSessionByCode(String sessionCode) {
        return agentSessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new NoSuchElementException("session not found: " + sessionCode));
    }

    /**
     * 按 sessionCode 获取会话，并校验其归属于指定的 agentCode。
     */
    public AgentSession getSessionByAgentAndCode(String agentCode, String sessionCode) {
        AgentSession session = getSessionByCode(sessionCode);
        if (!agentCode.equals(session.getAgentCode())) {
            throw new IllegalArgumentException("session does not belong to agent: " + agentCode);
        }
        return session;
    }

    /**
     * 获取会话摘要信息。
     */
    public SessionSummary getSessionSummary(String agentCode, String sessionCode) {
        AgentSession session = getSessionByAgentAndCode(agentCode, sessionCode);
        String snippet = agentSessionSummaryRepository.findBySessionCode(sessionCode)
                .map(AgentSummary::getContent)
                .orElseGet(() -> buildSnippetFromLastMessage(sessionCode));
        return new SessionSummary(session.getTitle(), snippet, session.getTurnCount());
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(String sessionCode) {
        AgentSession session = getSessionByCode(sessionCode);
        session.archive();
        agentSessionRepository.save(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void archive(String agentCode, String sessionCode) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        archive(sessionCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unarchive(String sessionCode) {
        AgentSession session = getSessionByCode(sessionCode);
        session.unarchive();
        agentSessionRepository.save(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void unarchive(String agentCode, String sessionCode) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        unarchive(sessionCode);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String sessionCode) {
        AgentSession session = getSessionByCode(sessionCode);
        session.markDeleted();
        agentSessionRepository.save(session);
    }

    @Transactional(rollbackFor = Exception.class)
    public void delete(String agentCode, String sessionCode) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        delete(sessionCode);
    }

    private AgentSession appendMessageInternal(String sessionCode, AgentMessage message) {
        AgentSession session = agentSessionRepository.findWithLockByCode(sessionCode)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionCode));
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        if (!sessionCode.equals(message.getSessionCode())) {
            throw new IllegalArgumentException("message sessionCode does not match sessionCode");
        }
        if (message.getTurnCode() == null || message.getTurnCode().isBlank()) {
            throw new IllegalArgumentException("message turnCode cannot be blank");
        }
        if (message.getContentBlockType() == null) {
            throw new IllegalArgumentException("message contentBlockType cannot be null");
        }
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.COMPLETED);
        }
        boolean isNewTurn = !agentMessageRepository.existsBySessionCodeAndTurnCode(sessionCode, message.getTurnCode());
        int nextMessageIndex = agentMessageRepository.findTopBySessionCodeOrderByMessageIndexDesc(sessionCode)
                .map(lastMessage -> lastMessage.getMessageIndex() + 1)
                .orElse(1);
        message.assignMessageIndex(nextMessageIndex);
        agentMessageRepository.save(message);
        session.appendMessage(Instant.now());
        if (isNewTurn) {
            session.incrementTurnCount();
        }
        return agentSessionRepository.save(session);
    }

    private String buildSnippetFromLastMessage(String sessionCode) {
        return agentMessageRepository.findTopBySessionCodeOrderByMessageIndexDesc(sessionCode)
                .map(AgentMessage::getPayloadJson)
                .map(json -> {
                    String text = extractTextFromPayload(json);
                    if (text != null && text.length() > SNIPPET_MAX_LENGTH) {
                        return text.substring(0, SNIPPET_MAX_LENGTH) + "...";
                    }
                    return text;
                })
                .orElse(null);
    }

    private String extractTextFromPayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            var parsed = com.alibaba.fastjson2.JSON.parseObject(payloadJson);
            if (parsed != null && parsed.containsKey("text")) {
                return parsed.getString("text");
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public record MessageTurn(String turnCode, List<AgentMessage> blocks) {
    }

    public record CursorResult<T>(List<T> data, String nextCursor, boolean hasMore) {
    }

    public record SessionSummary(String title, String lastMessageSnippet, int turnCount) {
    }
}
