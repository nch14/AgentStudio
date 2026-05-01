package com.chenhaonee.agents.claudecode.process;

/**
 * Chat 进程池已满且全部 ACTIVE 时抛出，由调用方转为友好错误提示。
 */
public class TooManyActiveChatsException extends RuntimeException {

    public TooManyActiveChatsException(String message) {
        super(message);
    }
}
