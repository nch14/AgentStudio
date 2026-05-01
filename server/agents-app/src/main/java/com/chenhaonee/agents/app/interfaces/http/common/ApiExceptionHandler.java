package com.chenhaonee.agents.app.interfaces.http.common;

import io.swagger.v3.oas.annotations.Hidden;
import java.time.Instant;
import java.util.Map;
import java.util.NoSuchElementException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * 统一接口异常输出。
 */
@Hidden
@ControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoSuchElementException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("NOT_FOUND", exception.getMessage()));
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<Map<String, Object>> handleBadRequest(RuntimeException exception) {
        return ResponseEntity.badRequest()
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorBody("BAD_REQUEST", exception.getMessage()));
    }

    private Map<String, Object> errorBody(String code, String message) {
        return Map.of(
                "error", code,
                "message", message,
                "timestamp", Instant.now().toString());
    }
}
