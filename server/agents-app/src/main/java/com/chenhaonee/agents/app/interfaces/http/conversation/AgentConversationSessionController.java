package com.chenhaonee.agents.app.interfaces.http.conversation;

import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.PageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentSessionDTO;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.AgentSessionMessageDTO;
import com.chenhaonee.agents.app.interfaces.http.conversation.dto.RenameAgentSessionRequest;
import com.chenhaonee.agents.domain.session.model.AgentMessage;
import com.chenhaonee.agents.domain.session.model.AgentSession;
import com.chenhaonee.agents.domain.session.service.AgentSessionDomainService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 会话管理接口。
 */
@Tag(name = "Agent Session", description = "Agent 会话管理接口")
@RestController
@RequestMapping("/api/v1/agents/{agentCode}/sessions")
public class AgentConversationSessionController {

    private final AgentSessionDomainService agentSessionDomainService;

    public AgentConversationSessionController(AgentSessionDomainService agentSessionDomainService) {
        this.agentSessionDomainService = agentSessionDomainService;
    }

    @Operation(summary = "分页查询会话列表")
    @GetMapping
    public PageResponse<AgentSessionDTO> list(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "是否只查询归档会话") @RequestParam(defaultValue = "false") boolean archived) {
        try {
            return PageResponse.from(
                    agentSessionDomainService.listSessionsByAgent(page, size, agentCode, archived)
                            .map(this::toSessionResponse));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
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

    @Operation(summary = "分页查询会话消息列表")
    @GetMapping("/{sessionCode}/messages")
    public PageResponse<AgentSessionMessageDTO> messages(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "会话编码") @PathVariable String sessionCode,
            @Parameter(description = "页码，从 0 开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量") @RequestParam(defaultValue = "50") int size) {
        try {
            PageRequest pageable = PageRequest.of(page, size);
            return PageResponse.from(
                    agentSessionDomainService.listMessages(agentCode, sessionCode, pageable)
                            .map(this::toMessageResponse));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
        }
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
                session.getMessageCount(),
                session.getLastMessageTime() != null ? session.getLastMessageTime().toString() : null,
                session.isArchived(),
                session.getCreateTime() != null ? session.getCreateTime().toString() : null,
                session.getUpdateTime() != null ? session.getUpdateTime().toString() : null
        );
    }

    private AgentSessionMessageDTO toMessageResponse(AgentMessage message) {
        return new AgentSessionMessageDTO(
                message.getCode(),
                message.getRole() == null ? null : message.getRole().name(),
                message.getProtocolTypeCode(),
                message.getStatusCode(),
                message.getPayloadJson(),
                message.getErrorPayloadJson(),
                message.getExternalMessageId(),
                message.getMessageIndex()
        );
    }
}
