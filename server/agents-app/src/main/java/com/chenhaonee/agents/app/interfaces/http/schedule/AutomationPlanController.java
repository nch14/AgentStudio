package com.chenhaonee.agents.app.interfaces.http.schedule;

import com.chenhaonee.agents.app.application.schedule.AutomationPlanCommandApplicationService;
import com.chenhaonee.agents.app.application.schedule.AutomationPlanQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.PageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.schedule.dto.AutomationPlanCreateRequest;
import com.chenhaonee.agents.app.interfaces.http.schedule.dto.AutomationPlanDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.schedule.dto.AutomationPlanListItemDTO;
import com.chenhaonee.agents.app.interfaces.http.schedule.dto.AutomationPlanUpdateRequest;
import com.chenhaonee.agents.common.domain.Status;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan;
import com.chenhaonee.agents.domain.schedule.model.AutomationPlan.DeliveryMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 自动化计划管理接口。
 */
@Tag(name = "AutomationPlan", description = "自动化计划管理")
@RestController
@RequestMapping("/api/v1/automation-plans")
public class AutomationPlanController {

    private final AutomationPlanQueryApplicationService queryApplicationService;
    private final AutomationPlanCommandApplicationService commandApplicationService;
    private final AutomationPlanHttpAssembler assembler;

    public AutomationPlanController(AutomationPlanQueryApplicationService queryApplicationService,
                                    AutomationPlanCommandApplicationService commandApplicationService,
                                    AutomationPlanHttpAssembler assembler) {
        this.queryApplicationService = queryApplicationService;
        this.commandApplicationService = commandApplicationService;
        this.assembler = assembler;
    }

    @Operation(summary = "分页查询自动化计划列表")
    @GetMapping
    public PageResponse<AutomationPlanListItemDTO> list(
            @Parameter(description = "页码，从 0 开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "状态过滤（ENABLED/DISABLED）")
            @RequestParam(required = false) String status) {
        try {
            Status statusEnum = status != null ? Status.valueOf(status) : null;
            return PageResponse.from(
                    queryApplicationService.listPlans(page, size, statusEnum)
                            .map(assembler::toListItemResponse));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询自动化计划详情")
    @GetMapping("/{planCode}")
    public Response<AutomationPlanDetailDTO> detail(
            @Parameter(description = "计划编码") @PathVariable String planCode) {
        try {
            return Response.success(
                    assembler.toDetailResponse(queryApplicationService.getPlan(planCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "创建自动化计划")
    @PostMapping
    public Response<AutomationPlanDetailDTO> create(@Valid @RequestBody AutomationPlanCreateRequest request) {
        try {
            DeliveryMode deliveryMode = DeliveryMode.valueOf(request.deliveryMode());
            AutomationPlan plan = commandApplicationService.createPlan(
                    request.name(), request.agentCode(), request.instruction(),
                    request.scheduleRule(), deliveryMode);
            return Response.success(assembler.toDetailResponse(plan));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "修改自动化计划")
    @PutMapping("/{planCode}")
    public Response<AutomationPlanDetailDTO> update(
            @Parameter(description = "计划编码") @PathVariable String planCode,
            @Valid @RequestBody AutomationPlanUpdateRequest request) {
        try {
            DeliveryMode deliveryMode = request.deliveryMode() != null ? DeliveryMode.valueOf(request.deliveryMode()) : null;
            AutomationPlan plan = commandApplicationService.updatePlan(
                    planCode, request.name(), request.instruction(),
                    request.scheduleRule(), deliveryMode);
            return Response.success(assembler.toDetailResponse(plan));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "启用自动化计划")
    @PostMapping("/{planCode}/enable")
    public Response<AutomationPlanDetailDTO> enable(
            @Parameter(description = "计划编码") @PathVariable String planCode) {
        try {
            return Response.success(
                    assembler.toDetailResponse(commandApplicationService.enablePlan(planCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "禁用自动化计划")
    @PostMapping("/{planCode}/disable")
    public Response<AutomationPlanDetailDTO> disable(
            @Parameter(description = "计划编码") @PathVariable String planCode) {
        try {
            return Response.success(
                    assembler.toDetailResponse(commandApplicationService.disablePlan(planCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "删除自动化计划")
    @PostMapping("/{planCode}/delete")
    public Response<Void> delete(
            @Parameter(description = "计划编码") @PathVariable String planCode) {
        try {
            commandApplicationService.deletePlan(planCode);
            return Response.successWithMessage("自动化计划已删除");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }
}
