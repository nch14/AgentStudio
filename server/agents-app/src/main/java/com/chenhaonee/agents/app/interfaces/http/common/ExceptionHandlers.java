package com.chenhaonee.agents.app.interfaces.http.common;

import java.util.NoSuchElementException;

/**
 * 控制器异常处理工具类。
 * 将已知业务异常转换为统一错误信息，避免异常直接泄露到前端。
 */
public final class ExceptionHandlers {

    private ExceptionHandlers() {
    }

    /**
     * 将异常转换为统一错误信息。
     * <ul>
     *     <li>{@link NoSuchElementException}、{@link IllegalArgumentException}、{@link IllegalStateException} → 400</li>
     *     <li>其他异常 → 500</li>
     * </ul>
     */
    public static Response.ErrorInfo handleException(Exception e) {
        if (e instanceof NoSuchElementException || e instanceof IllegalArgumentException || e instanceof IllegalStateException) {
            return new Response.ErrorInfo(400, e.getMessage());
        }
        return new Response.ErrorInfo(500, "系统内部异常：" + e.getMessage());
    }
}
