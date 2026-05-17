package com.chenhaonee.agents.app.interfaces.http.notify;

import com.chenhaonee.agents.app.application.notify.NotifyConfigCommandApplicationService;
import com.chenhaonee.agents.app.application.notify.NotifyConfigQueryApplicationService;
import com.chenhaonee.agents.app.application.notify.NotifyConfigView;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig;
import com.chenhaonee.agents.domain.notify.model.NotifyConfig.DeliveryMode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 通知事件配置管理接口。
 */
@Tag(name = "NotifyConfig", description = "通知事件配置管理")
@RestController
@RequestMapping("/api/v1/notify-configs")
@RequiredArgsConstructor
public class NotifyConfigController {

    private final NotifyConfigQueryApplicationService queryApplicationService;
    private final NotifyConfigCommandApplicationService commandApplicationService;

    @Operation(summary = "查询通知事件配置列表")
    @GetMapping
    public Response<List<NotifyConfigDto>> list() {
        try {
            List<NotifyConfigDto> dtos = queryApplicationService.listConfigs().stream()
                    .map(NotifyConfigDto::from)
                    .toList();
            return Response.success(dtos);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询通知事件配置详情")
    @GetMapping("/{eventCode}")
    public Response<NotifyConfigDto> detail(
            @Parameter(description = "通知事件编码") @PathVariable String eventCode) {
        try {
            NotificationEvent event = NotificationEvent.fromCode(eventCode);
            return Response.success(NotifyConfigDto.from(queryApplicationService.getConfig(event)));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "修改通知事件配置")
    @PutMapping("/{eventCode}")
    public Response<NotifyConfigDto> update(
            @Parameter(description = "通知事件编码") @PathVariable String eventCode,
            @Valid @RequestBody NotifyConfigUpdateRequest request) {
        try {
            NotificationEvent event = NotificationEvent.fromCode(eventCode);
            DeliveryMode deliveryMode = commandApplicationService.parseDeliveryMode(request.deliveryMode());
            NotifyConfig config = commandApplicationService.updateConfig(
                    event, request.enabled(), deliveryMode, request.channels());
            return Response.success(NotifyConfigDto.from(queryApplicationService.getConfig(config.getEvent())));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    /**
     * 通知事件配置 DTO。
     */
    public record NotifyConfigDto(
            @Schema(description = "事件分组编码") String groupCode,
            @Schema(description = "事件分组名称") String groupName,
            @Schema(description = "事件编码") String eventCode,
            @Schema(description = "事件名称") String eventName,
            @Schema(description = "事件说明") String description,
            @Schema(description = "是否启用") boolean enabled,
            @Schema(description = "投递模式（INSTANT/MERGED）") String deliveryMode,
            @Schema(description = "启用的通知渠道，空数组表示自动推导") List<String> channels,
            @Schema(description = "是否已有配置记录") boolean configured,
            @Schema(description = "创建时间") String createTime,
            @Schema(description = "更新时间") String updateTime) {

        static NotifyConfigDto from(NotifyConfigView v) {
            return new NotifyConfigDto(
                    v.groupCode(),
                    v.groupName(),
                    v.eventCode(),
                    v.eventName(),
                    v.description(),
                    v.enabled(),
                    v.deliveryMode(),
                    v.channels(),
                    v.configured(),
                    v.createTime(),
                    v.updateTime());
        }
    }

    /**
     * 修改通知事件配置请求。
     */
    public record NotifyConfigUpdateRequest(
            @Schema(description = "是否启用")
            Boolean enabled,

            @Schema(description = "投递模式（INSTANT/MERGED）")
            String deliveryMode,

            @Schema(description = "启用的通知渠道，空数组表示自动推导", example = "[\"BARK\", \"EMAIL\"]")
            List<String> channels) {
    }
}
