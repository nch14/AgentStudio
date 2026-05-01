package com.chenhaonee.agents.app.interfaces.http.common;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.HttpStatus;

/**
 * 统一 HTTP 响应包装。
 */
public class Response<T> {

    @Schema(description = "响应数据")
    private T data;

    @Schema(description = "是否成功", example = "true")
    private boolean success;

    @Schema(description = "业务状态码，200表示成功", example = "200")
    private int code = 200;

    @Schema(description = "错误信息或成功提示", example = "Success")
    private String errorMessage;

    public Response() {
    }

    public Response(T data, boolean success, int code, String errorMessage) {
        this.data = data;
        this.success = success;
        this.code = code;
        this.errorMessage = errorMessage;
    }

    public static <T> Response<T> success(T data) {
        return new Response<>(data, true, HttpStatus.OK.value(), "Success");
    }

    public static Response<Void> successWithMessage(String message) {
        return new Response<>(null, true, HttpStatus.OK.value(), message);
    }

    public static Response<Void> success() {
        return new Response<>(null, true, HttpStatus.OK.value(), "Success");
    }

    public static <T> Response<T> error(int code, String message) {
        return new Response<>(null, false, code, message);
    }

    public static <T> Response<T> error(ErrorInfo errorInfo) {
        return new Response<>(null, false, errorInfo.code(), errorInfo.message());
    }

    /**
     * 统一错误信息载体。
     */
    public record ErrorInfo(int code, String message) {
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
