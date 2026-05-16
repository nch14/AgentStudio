package com.chenhaonee.agents.app.interfaces.http.conversation;

import com.alibaba.fastjson2.JSON;
import com.chenhaonee.agents.app.application.conversation.ActiveStreamRegistry;
import com.chenhaonee.agents.app.application.conversation.SseKeepAlive;
import com.chenhaonee.agents.app.interfaces.http.common.CursorPageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.PageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentSessionBlockDTO;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentSessionDTO;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentSessionSummaryDTO;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentSessionTurnDTO;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.RenameAgentSessionRequest;
import com.chenhaonee.agents.connect.spi.model.MessagesEvent;
import com.chenhaonee.agents.domain.session.model.AgentMessage;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.model.SessionScene;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.WebRequest;
import reactor.core.publisher.Flux;

/**
 * Agent 会话管理接口。
 */
@Tag(name = "Agent Session", description = "Agent 会话管理接口")
@RestController
@RequestMapping("/api/v1/agents/{agentCode}/sessions")
public class AgentConversationSessionController {

    private final AgentSessionDomainService agentSessionDomainService;
    private final ActiveStreamRegistry activeStreamRegistry;

    public AgentConversationSessionController(
            AgentSessionDomainService agentSessionDomainService,
            ActiveStreamRegistry activeStreamRegistry) {
        this.agentSessionDomainService = agentSessionDomainService;
        this.activeStreamRegistry = activeStreamRegistry;
    }

    @Operation(summary = "分页查询会话列表")
    @GetMapping
    public ResponseEntity<PageResponse<AgentSessionDTO>> list(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "是否只查询归档会话") @RequestParam(defaultValue = "false") boolean archived,
            @Parameter(description = "场景类型：CHAT / TASK") @RequestParam(defaultValue = "CHAT") SessionScene scene,
            WebRequest webRequest) {
        try {
            Page<AgentSession> pageResult = agentSessionDomainService.listSessionsByAgent(page, size, agentCode, archived, scene);
            long lastModifiedMs = pageResult.getContent().stream()
                    .map(AgentSession::getUpdateTime)
                    .filter(java.util.Objects::nonNull)
                    .mapToLong(java.util.Date::getTime)
                    .max()
                    .orElse(0L);
            String etag = "\"sessions-" + Long.toHexString(lastModifiedMs) + "\"";
            if (webRequest.checkNotModified(etag)) {
                return ResponseEntity.status(304).build();
            }
            PageResponse<AgentSessionDTO> body = PageResponse.from(pageResult.map(this::toSessionResponse));
            return ResponseEntity.ok()
                    .eTag(etag)
                    .cacheControl(CacheControl.noCache().mustRevalidate().cachePrivate())
                    .body(body);
        } catch (Exception e) {
            return ResponseEntity.ok(PageResponse.errorPage(ExceptionHandlers.handleException(e)));
        }
    }

    @Operation(summary = "查看会话详情")
    @GetMapping("/{sessionCode}")
    public Response<AgentSessionDTO> detail(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode) {
        try {
            AgentSession session = agentSessionDomainService.getSessionByAgentAndCode(agentCode, sessionCode);
            return Response.success(toSessionResponse(session));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询会话摘要")
    @GetMapping("/{sessionCode}/summary")
    public Response<AgentSessionSummaryDTO> summary(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode) {
        try {
            AgentSessionDomainService.SessionSummary summary =
                    agentSessionDomainService.getSessionSummary(agentCode, sessionCode);
            return Response.success(new AgentSessionSummaryDTO(
                    summary.title(), summary.lastMessageSnippet(), summary.turnCount()));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "分页查询会话消息列表（按 turn 聚合，page 分页）")
    @GetMapping("/{sessionCode}/messages")
    public PageResponse<AgentSessionTurnDTO> messages(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页 turn 数量") @RequestParam(defaultValue = "50") int size) {
        try {
            PageRequest pageable = PageRequest.of(page, size);
            return PageResponse.from(
                    agentSessionDomainService.listMessageTurns(agentCode, sessionCode, pageable)
                            .map(this::toTurnResponse));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "游标分页查询会话消息列表（按 turn 聚合）")
    @GetMapping("/{sessionCode}/messages/cursor")
    public ResponseEntity<CursorPageResponse<AgentSessionTurnDTO>> messagesByCursor(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode,
            @Parameter(description = "游标值（messageIndex），不传则返回最新消息") @RequestParam(required = false) Integer cursor,
            @Parameter(description = "每页 turn 数量") @RequestParam(defaultValue = "20") int size,
            WebRequest webRequest) {
        try {
            AgentSession session = agentSessionDomainService.getSessionByAgentAndCode(agentCode, sessionCode);
            String etag = buildSessionETag(session);
            if (webRequest.checkNotModified(etag)) {
                return ResponseEntity.status(304).build();
            }
            AgentSessionDomainService.CursorResult<AgentSessionDomainService.MessageTurn> result =
                    agentSessionDomainService.listMessageTurnsByCursor(agentCode, sessionCode, cursor, size);
            List<AgentSessionTurnDTO> turns = result.data().stream()
                    .map(this::toTurnResponse)
                    .toList();
            return ResponseEntity.ok()
                    .eTag(etag)
                    .cacheControl(CacheControl.noCache().mustRevalidate().cachePrivate())
                    .body(CursorPageResponse.of(turns, result.nextCursor(), result.hasMore()));
        } catch (Exception e) {
            return ResponseEntity.ok(CursorPageResponse.errorCursor(ExceptionHandlers.handleException(e)));
        }
    }

    @Operation(summary = "流式恢复：先以 historical_block 回放历史，再接续活跃流")
    @GetMapping(value = "/{sessionCode}/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<ServerSentEvent<String>>> streamResume(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode,
            @Parameter(description = "从哪条消息开始（messageIndex），不传则只接续活跃流") @RequestParam(required = false) Integer fromIndex) {
        try {
            agentSessionDomainService.getSessionByAgentAndCode(agentCode, sessionCode);

            Flux<ServerSentEvent<String>> historical = Flux.empty();
            if (fromIndex != null) {
                List<AgentMessage> historicalMessages = agentSessionDomainService
                        .listMessagesFromIndex(agentCode, sessionCode, fromIndex);
                historical = Flux.fromIterable(historicalMessages)
                        .map(this::toHistoricalSse);
            }

            Flux<ServerSentEvent<String>> live = activeStreamRegistry.get(sessionCode)
                    .map(stream -> stream.map(this::toSseFromEvent))
                    .orElse(Flux.empty());

            Flux<ServerSentEvent<String>> combined = Flux.concat(historical, live);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(SseKeepAlive.wrap(combined));
        } catch (IllegalArgumentException e) {
            Flux<ServerSentEvent<String>> errorFlux = Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("{\"type\":\"error\",\"message\":\"" + e.getMessage() + "\"}")
                    .build());
            return ResponseEntity.badRequest()
                    .contentType(MediaType.TEXT_EVENT_STREAM)
                    .body(errorFlux);
        }
    }

    @Operation(summary = "中断会话的活跃流式对话")
    @DeleteMapping("/{sessionCode}/stream")
    public ResponseEntity<Response<InterruptResult>> interruptStream(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode) {
        agentSessionDomainService.getSessionByAgentAndCode(agentCode, sessionCode);
        boolean interrupted = activeStreamRegistry.cancel(sessionCode);
        return ResponseEntity.ok(Response.success(new InterruptResult(interrupted)));
    }

    public record InterruptResult(boolean interrupted) {
    }

    @Operation(summary = "重命名会话")
    @PostMapping("/{sessionCode}/rename")
    public Response<AgentSessionDTO> rename(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode,
            @Valid @RequestBody RenameAgentSessionRequest request) {
        try {
            AgentSession session = agentSessionDomainService.rename(agentCode, sessionCode, request.title());
            return Response.success(toSessionResponse(session));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "归档会话")
    @PostMapping("/{sessionCode}/archive")
    public Response<Void> archive(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode) {
        try {
            agentSessionDomainService.archive(agentCode, sessionCode);
            return Response.successWithMessage("会话已归档");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "取消归档会话")
    @PostMapping("/{sessionCode}/unarchive")
    public Response<Void> unarchive(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode) {
        try {
            agentSessionDomainService.unarchive(agentCode, sessionCode);
            return Response.successWithMessage("会话已取消归档");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "删除会话")
    @PostMapping("/{sessionCode}/delete")
    public Response<Void> delete(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode) {
        try {
            agentSessionDomainService.delete(agentCode, sessionCode);
            return Response.successWithMessage("会话已删除");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    private AgentSessionDTO toSessionResponse(AgentSession session) {
        return new AgentSessionDTO(
                session.getCode(),
                session.getTitle(),
                session.getAgentCode(),
                session.getScene() != null ? session.getScene().name() : null,
                session.getMessageCount(),
                session.getTurnCount(),
                session.getLastMessageTime() != null ? session.getLastMessageTime().toString() : null,
                session.isArchived(),
                session.getCreateTime() != null ? session.getCreateTime().toString() : null,
                session.getUpdateTime() != null ? session.getUpdateTime().toString() : null
        );
    }

    private AgentSessionTurnDTO toTurnResponse(AgentSessionDomainService.MessageTurn turn) {
        return new AgentSessionTurnDTO(
                turn.turnCode(),
                turn.blocks().stream().map(this::toBlockResponse).toList()
        );
    }

    private AgentSessionBlockDTO toBlockResponse(AgentMessage message) {
        return new AgentSessionBlockDTO(
                message.getCode(),
                message.getRole() == null ? null : message.getRole().name(),
                message.getContentBlockType() == null ? null : message.getContentBlockType().name(),
                message.getMessageIndex(),
                parseJson(message.getPayloadJson()),
                parseJson(message.getErrorPayloadJson()),
                message.getExternalMessageId()
        );
    }

    private Object parseJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        return JSON.parse(json);
    }

    private String buildSessionETag(AgentSession session) {
        long ts = session.getUpdateTime() != null ? session.getUpdateTime().getTime() : 0L;
        return "\"msg-" + session.getCode() + "-" + Long.toHexString(ts) + "\"";
    }

    private ServerSentEvent<String> toHistoricalSse(AgentMessage message) {
        AgentSessionBlockDTO block = toBlockResponse(message);
        return ServerSentEvent.<String>builder()
                .event("historical_block")
                .data(JSON.toJSONString(block))
                .build();
    }

    private ServerSentEvent<String> toSseFromEvent(MessagesEvent event) {
        return ServerSentEvent.<String>builder()
                .event(event.eventType())
                .data(event.dataJson())
                .build();
    }
}
