package com.chenhaonee.agents.app.interfaces.http.conversation;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService;
import com.chenhaonee.agents.app.application.conversation.AnthropicMessagesService.AnthropicMessagesResult;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentUserMessageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * 产品级 Agent 消息发送入口。
 */
@Tag(name = "Agent Message", description = "产品级 Agent 消息发送接口")
@RestController
@RequestMapping("/api/v1/agents/{agentCode}/messages")
@RequiredArgsConstructor
public class AgentConversationMessageController {

    private final AnthropicMessagesService anthropicMessagesService;

    @Operation(summary = "发送一条用户消息（含可选附件）")
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> create(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @RequestHeader(value = "X-Agent-Session-Code", required = false) String sessionCode,
            @RequestBody AgentUserMessageRequest request
    ) {
        try {
            AnthropicMessagesResult result = anthropicMessagesService.createStreaming(
                    agentCode, sessionCode, request.content(), request.attachments());
            Flux<ServerSentEvent<String>> events = result.events();
            if (events == null) {
                throw new IllegalStateException("streaming events missing");
            }
            return withCommonHeaders(ResponseEntity.ok(), agentCode, result.sessionCode())
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(events);
        } catch (IllegalArgumentException e) {
            return buildSseError(HttpStatus.BAD_REQUEST, agentCode, sessionCode, e.getMessage());
        } catch (IllegalStateException e) {
            return buildSseError(HttpStatus.CONFLICT, agentCode, sessionCode, e.getMessage());
        }
    }

    private ResponseEntity<Flux<ServerSentEvent<String>>> buildSseError(
            HttpStatus status,
            String agentCode,
            String sessionCode,
            String message
    ) {
        JSONObject error = new JSONObject();
        error.put("type", "error");
        error.put("message", message);
        Flux<ServerSentEvent<String>> eventFlux = Flux.just(ServerSentEvent.<String>builder()
                .event("error")
                .data(JSON.toJSONString(error))
                .build());
        return withCommonHeaders(ResponseEntity.status(status), agentCode, sessionCode)
                .contentType(MediaType.TEXT_EVENT_STREAM)
                .body(eventFlux);
    }

    private ResponseEntity.BodyBuilder withCommonHeaders(
            ResponseEntity.BodyBuilder builder,
            String agentCode,
            String sessionCode
    ) {
        builder.header("X-Agent-Code", agentCode);
        if (StringUtils.isNotBlank(sessionCode)) {
            builder.header("X-Agent-Session-Code", sessionCode);
        }
        return builder;
    }
}
