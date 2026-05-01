package com.chenhaonee.agents.domain.session.service;

import com.chenhaonee.agents.domain.session.model.AgentMessage;
import com.chenhaonee.agents.domain.session.model.MessageStatus;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.repository.AgentMessageRepository;
import com.chenhaonee.agents.domain.session.repository.AgentSessionRepository;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Agent 会话领域服务。
 */
@Service
public class AgentSessionDomainService {

    private final AgentMessageRepository agentMessageRepository;
    private final AgentSessionRepository agentSessionRepository;

    public AgentSessionDomainService(
            AgentMessageRepository agentMessageRepository,
            AgentSessionRepository agentSessionRepository
    ) {
        this.agentMessageRepository = agentMessageRepository;
        this.agentSessionRepository = agentSessionRepository;
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
    public AgentMessage completeMessage(String messageCode, String payloadJson, String externalMessageId) {
        AgentMessage message = getMessageByCode(messageCode);
        message.complete(payloadJson, externalMessageId);
        return agentMessageRepository.save(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentMessage failMessage(String messageCode, String partialPayloadJson, String errorPayloadJson) {
        AgentMessage message = getMessageByCode(messageCode);
        message.fail(partialPayloadJson, errorPayloadJson);
        return agentMessageRepository.save(message);
    }

    @Transactional(rollbackFor = Exception.class)
    public AgentMessage cancelMessage(String messageCode, String partialPayloadJson, String errorPayloadJson) {
        AgentMessage message = getMessageByCode(messageCode);
        message.cancel(partialPayloadJson, errorPayloadJson);
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

    public org.springframework.data.domain.Page<AgentMessage> listMessages(String sessionCode, org.springframework.data.domain.Pageable pageable) {
        if (!agentSessionRepository.existsByCode(sessionCode)) {
            throw new IllegalArgumentException("session not found: " + sessionCode);
        }
        return agentMessageRepository.findBySessionCodeOrderByMessageIndexAsc(sessionCode, pageable);
    }

    public org.springframework.data.domain.Page<AgentMessage> listMessages(String agentCode, String sessionCode, org.springframework.data.domain.Pageable pageable) {
        getSessionByAgentAndCode(agentCode, sessionCode);
        return listMessages(sessionCode, pageable);
    }

    public AgentMessage getMessageByCode(String messageCode) {
        return agentMessageRepository.findByCode(messageCode)
                .orElseThrow(() -> new NoSuchElementException("message not found: " + messageCode));
    }

    public Page<AgentSession> listSessions(int page, int size, boolean archived) {
        PageRequest pageable = PageRequest.of(page, size);
        if (archived) {
            return agentSessionRepository.findByArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(pageable);
        }
        return agentSessionRepository.findByArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(pageable);
    }

    public Page<AgentSession> listSessionsByAgent(int page, int size, String agentCode, boolean archived) {
        PageRequest pageable = PageRequest.of(page, size);
        if (archived) {
            return agentSessionRepository.findByAgentCodeAndArchivedTrueAndValidIsTrueOrderByUpdateTimeDesc(agentCode, pageable);
        }
        return agentSessionRepository.findByAgentCodeAndArchivedFalseAndValidIsTrueOrderByUpdateTimeDesc(agentCode, pageable);
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
        AgentSession session = agentSessionRepository.findByCode(sessionCode)
                .orElseThrow(() -> new IllegalArgumentException("session not found: " + sessionCode));
        if (message == null) {
            throw new IllegalArgumentException("message cannot be null");
        }
        if (!sessionCode.equals(message.getSessionCode())) {
            throw new IllegalArgumentException("message sessionCode does not match sessionCode");
        }
        if (message.getStatus() == null) {
            message.setStatus(MessageStatus.COMPLETED);
        }
        int nextMessageIndex = agentMessageRepository.findTopBySessionCodeOrderByMessageIndexDesc(sessionCode)
                .map(lastMessage -> lastMessage.getMessageIndex() + 1)
                .orElse(1);
        message.assignMessageIndex(nextMessageIndex);
        agentMessageRepository.save(message);
        session.appendMessage(Instant.now());
        return agentSessionRepository.save(session);
    }
}
