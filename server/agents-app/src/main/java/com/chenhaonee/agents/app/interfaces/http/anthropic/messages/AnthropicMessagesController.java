package com.chenhaonee.agents.app.interfaces.http.anthropic.messages;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService.AnthropicMessagesResult;
import com.chenhaonee.agents.common.domain.Identity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Anthropic Messages 兼容接口。
 */
@Tag(name = "Anthropic Messages", description = "Anthropic Messages API 兼容接口")
@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class AnthropicMessagesController {

    private final AnthropicMessagesService anthropicMessagesService;

    @Operation(summary = "创建 Messages 对话")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> create(
            @RequestHeader("X-Agent-Code") String agentCode,
            @RequestHeader(value = "X-Agent-Session-Code", required = false) String sessionCode,
            @RequestBody String requestJson
    ) {
        String requestId = "req_" + Identity.newIdentity().value();
        try {
            AnthropicMessagesResult result = anthropicMessagesService.create(agentCode, sessionCode, requestJson);
            if (result.events() != null) {
                return withAgentHeaders(ResponseEntity.ok(), agentCode, result.sessionCode(), requestId)
                        .contentType(MediaType.TEXT_EVENT_STREAM)
                        .body(result.events());
            }
            return withAgentHeaders(ResponseEntity.ok(), agentCode, result.sessionCode(), requestId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(result.messageJson());
        } catch (IllegalArgumentException e) {
            return buildJsonError(HttpStatus.BAD_REQUEST, e.getMessage(), agentCode, sessionCode, requestId);
        } catch (IllegalStateException e) {
            return buildJsonError(HttpStatus.CONFLICT, e.getMessage(), agentCode, sessionCode, requestId);
        }
    }

    private ResponseEntity<String> buildJsonError(
            HttpStatus status,
            String message,
            String agentCode,
            String sessionCode,
            String requestId
    ) {
        JSONObject body = new JSONObject();
        body.put("type", "error");
        JSONObject detail = new JSONObject();
        detail.put("type", "invalid_request_error");
        detail.put("message", message);
        body.put("error", detail);
        return withAgentHeaders(ResponseEntity.status(status), agentCode, sessionCode, requestId)
                .contentType(MediaType.APPLICATION_JSON)
                .body(JSON.toJSONString(body));
    }

    private ResponseEntity.BodyBuilder withAgentHeaders(
            ResponseEntity.BodyBuilder builder,
            String agentCode,
            String sessionCode,
            String requestId
    ) {
        builder.header("X-Agent-Code", agentCode);
        builder.header("request-id", requestId);
        if (StringUtils.isNotBlank(sessionCode)) {
            builder.header("X-Agent-Session-Code", sessionCode);
        }
        return builder;
    }
}
