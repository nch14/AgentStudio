package com.chenhaonee.agents.claudecode.process;

/**
 * 同一 session 上前一轮 turn 尚未结束，拒绝并发请求时抛出。
 */
public class SessionBusyException extends RuntimeException {

    public SessionBusyException(String sessionId) {
        super("Session is busy: " + sessionId);
    }
}
