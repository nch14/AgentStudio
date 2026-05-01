package com.chenhaonee.agents.app.interfaces.http.agent;

import com.chenhaonee.agents.app.application.agent.AgentCommandApplicationService;
import com.chenhaonee.agents.app.application.agent.AgentQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentCreateRequest;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentUpdateRequest;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.PageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Agent 管理接口。
 */
@Tag(name = "Agent", description = "Agent 管理")
@RestController
@RequestMapping("/api/v1/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentQueryApplicationService agentQueryApplicationService;
    private final AgentCommandApplicationService agentCommandApplicationService;
    private final AgentHttpAssembler agentHttpAssembler;

    @Operation(summary = "分页查询 Agent 列表")
    @GetMapping
    public PageResponse<AgentDetailDTO> list(
            @Parameter(description = "页码，从 0 开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "状态过滤（ENABLED/DISABLED）")
            @RequestParam(required = false) String status) {
        try {
            Status statusEnum = status != null ? Status.valueOf(status) : null;
            return PageResponse.from(
                    agentQueryApplicationService.listAgents(page, size, statusEnum)
                            .map(agentHttpAssembler::toDetailResponse));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查看 Agent 详情")
    @GetMapping("/{agentCode}")
    public Response<AgentDetailDTO> detail(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            Agent agent = agentQueryApplicationService.getAgentByCode(agentCode);
            return Response.success(agentHttpAssembler.toDetailResponse(agent));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "新建 Agent")
    @PostMapping
    public Response<AgentDetailDTO> create(@Valid @RequestBody AgentCreateRequest request) {
        try {
            Agent agent = agentCommandApplicationService.createAgent(
                    request.name(),
                    request.responsibility(),
                    AgentProvider.valueOf(request.provider()),
                    request.providerConfig());
            return Response.success(agentHttpAssembler.toDetailResponse(agent));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "修改 Agent")
    @PutMapping("/{agentCode}")
    public Response<AgentDetailDTO> update(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Valid @RequestBody AgentUpdateRequest request) {
        try {
            Agent agent = agentCommandApplicationService.updateAgent(
                    agentCode,
                    request.name(),
                    request.responsibility(),
                    request.providerConfig());
            return Response.success(agentHttpAssembler.toDetailResponse(agent));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "启用 Agent")
    @PostMapping("/{agentCode}/enable")
    public Response<AgentDetailDTO> enable(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            Agent agent = agentCommandApplicationService.enableAgent(agentCode);
            return Response.success(agentHttpAssembler.toDetailResponse(agent));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "禁用 Agent")
    @PostMapping("/{agentCode}/disable")
    public Response<AgentDetailDTO> disable(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            Agent agent = agentCommandApplicationService.disableAgent(agentCode);
            return Response.success(agentHttpAssembler.toDetailResponse(agent));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "删除 Agent")
    @DeleteMapping("/{agentCode}")
    public Response<Void> delete(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            agentCommandApplicationService.deleteAgent(agentCode);
            return Response.successWithMessage("Agent 已删除");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }
}
