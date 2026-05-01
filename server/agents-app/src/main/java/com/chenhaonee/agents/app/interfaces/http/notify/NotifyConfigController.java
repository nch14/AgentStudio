package com.chenhaonee.agents.app.interfaces.http.notify;

import com.chenhaonee.agents.app.application.notify.NotifyConfigCommandApplicationService;
import com.chenhaonee.agents.app.application.notify.NotifyConfigQueryApplicationService;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.PageResponse;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig.DeliveryMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
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
 * 通知配置管理接口。
 */
@Tag(name = "NotifyConfig", description = "通知配置管理")
@RestController
@RequestMapping("/api/v1/notify-configs")
@RequiredArgsConstructor
public class NotifyConfigController {

    private final NotifyConfigQueryApplicationService queryApplicationService;
    private final NotifyConfigCommandApplicationService commandApplicationService;

    @Operation(summary = "分页查询通知配置列表")
    @GetMapping
    public PageResponse<NotifyConfigDto> list(
            @Parameter(description = "页码，从 0 开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size) {
        try {
            return PageResponse.from(
                    queryApplicationService.listConfigs(page, size)
                            .map(NotifyConfigDto::from));
        } catch (Exception e) {
            return PageResponse.errorPage(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询通知配置详情")
    @GetMapping("/{configCode}")
    public Response<NotifyConfigDto> detail(
            @Parameter(description = "配置编码") @PathVariable String configCode) {
        try {
            return Response.success(
                    NotifyConfigDto.from(queryApplicationService.getConfig(configCode)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "创建通知配置")
    @PostMapping
    public Response<NotifyConfigDto> create(@Valid @RequestBody NotifyConfigCreateRequest request) {
        try {
            DeliveryMode deliveryMode = DeliveryMode.valueOf(request.deliveryMode());
            NotifyConfig config = commandApplicationService.createConfig(
                    request.name(), deliveryMode, request.channels());
            return Response.success(NotifyConfigDto.from(config));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "修改通知配置")
    @PutMapping("/{configCode}")
    public Response<NotifyConfigDto> update(
            @Parameter(description = "配置编码") @PathVariable String configCode,
            @Valid @RequestBody NotifyConfigUpdateRequest request) {
        try {
            DeliveryMode deliveryMode = request.deliveryMode() != null ? DeliveryMode.valueOf(request.deliveryMode()) : null;
            NotifyConfig config = commandApplicationService.updateConfig(
                    configCode, request.name(), deliveryMode, request.channels());
            return Response.success(NotifyConfigDto.from(config));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "删除通知配置")
    @DeleteMapping("/{configCode}")
    public Response<Void> delete(
            @Parameter(description = "配置编码") @PathVariable String configCode) {
        try {
            commandApplicationService.deleteConfig(configCode);
            return Response.successWithMessage("通知配置已删除");
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    /**
     * 通知配置 DTO。
     */
    public record NotifyConfigDto(
            @Schema(description = "配置编码") String configCode,
            @Schema(description = "配置名称") String name,
            @Schema(description = "投递模式（INSTANT/MERGED）") String deliveryMode,
            @Schema(description = "启用的通知渠道，逗号分隔") String channels,
            @Schema(description = "创建时间") String createTime,
            @Schema(description = "更新时间") String updateTime) {

        static NotifyConfigDto from(NotifyConfig c) {
            return new NotifyConfigDto(
                    c.getCode(),
                    c.getName(),
                    c.getDeliveryMode().name(),
                    c.getChannels(),
                    c.getCreateTime() != null ? c.getCreateTime().toString() : null,
                    c.getUpdateTime() != null ? c.getUpdateTime().toString() : null);
        }
    }

    /**
     * 创建通知配置请求。
     */
    public record NotifyConfigCreateRequest(
            @Schema(description = "配置名称", example = "任务完成通知")
            @NotBlank(message = "配置名称不能为空")
            String name,

            @Schema(description = "投递模式（INSTANT/MERGED）", example = "INSTANT")
            @NotBlank(message = "投递模式不能为空")
            String deliveryMode,

            @Schema(description = "启用的通知渠道，逗号分隔", example = "BARK,EMAIL")
            String channels) {
    }

    /**
     * 修改通知配置请求。
     */
    public record NotifyConfigUpdateRequest(
            @Schema(description = "配置名称")
            String name,

            @Schema(description = "投递模式（INSTANT/MERGED）")
            String deliveryMode,

            @Schema(description = "启用的通知渠道，逗号分隔")
            String channels) {
    }
}
