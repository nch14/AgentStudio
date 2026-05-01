package com.chenhaonee.agents.app.interfaces.http;

import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 消息中心接口。
 */
@Tag(name = "Notification", description = "消息中心")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    private final MessageCenter messageCenter;

    @Operation(summary = "分页查询消息列表")
    @GetMapping
    public Response<Page<NotificationDto>> list(
            @Parameter(description = "页码，从 0 开始")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页数量")
            @RequestParam(defaultValue = "20") int size) {
        try {
            return Response.success(messageCenter.getNotifications(page, size).map(NotificationDto::from));
        } catch (Exception e) {
            log.error("Failed to list notifications", e);
            return Response.error(500, e.getMessage());
        }
    }

    @Operation(summary = "未读消息数量")
    @GetMapping("/unread-count")
    public Response<Long> unreadCount() {
        try {
            return Response.success(messageCenter.getUnreadCount());
        } catch (Exception e) {
            log.error("Failed to get unread count", e);
            return Response.error(500, e.getMessage());
        }
    }

    @Operation(summary = "标记消息已读")
    @PostMapping("/{code}/read")
    public Response<Void> markRead(@PathVariable String code) {
        try {
            messageCenter.markRead(code);
            return Response.success();
        } catch (Exception e) {
            log.error("Failed to mark message read, code={}", code, e);
            return Response.error(500, e.getMessage());
        }
    }

    @Operation(summary = "发送通知（测试用）")
    @PostMapping("/send")
    public Response<Void> send(@RequestBody SendRequest request) {
        try {
            log.info("Trigger test notification, configCode={}, subject={}", request.configCode(), request.subject());
            messageCenter.send(request.configCode(), request.subject(), request.content());
            return Response.successWithMessage("通知发送已触发，请查看日志和消息列表");
        } catch (Exception e) {
            log.error("Failed to send notification, configCode={}", request.configCode(), e);
            return Response.error(500, e.getMessage());
        }
    }

    public record NotificationDto(
            String code,
            String configCode,
            String channel,
            String recipient,
            String subject,
            String content,
            String status,
            String errorMessage,
            String sentAt,
            String readAt,
            String createTime) {

        static NotificationDto from(Notification n) {
            return new NotificationDto(
                    n.getCode(),
                    n.getConfigCode(),
                    n.getChannel().name(),
                    n.getRecipient(),
                    n.getSubject(),
                    n.getContent(),
                    n.getStatus().name(),
                    n.getErrorMessage(),
                    n.getSentAt() != null ? n.getSentAt().toString() : null,
                    n.getReadAt() != null ? n.getReadAt().toString() : null,
                    n.getCreateTime() != null ? n.getCreateTime().toString() : null);
        }
    }

    public record SendRequest(
            @Schema(description = "通知配置编码", example = "questions_raised")
            String configCode,
            @Schema(description = "主题")
            String subject,
            @Schema(description = "内容")
            String content) {
    }
}
