package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.notify.model.NotificationEvent;
import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;

/**
 * Task 相关通知发送服务。负责整理用户可读模板变量并委托给 MessageCenter 处理。
 */
@Service
public class TaskNotificationService {

    private final MessageCenter messageCenter;

    public TaskNotificationService(MessageCenter messageCenter) {
        this.messageCenter = messageCenter;
    }

    public void notifyTaskWaiting(Task task, TaskTurn turn) {
        messageCenter.send(NotificationEvent.TURN_WAITING, variables(task, turn));
    }

    public void notifyTaskHanging(Task task, TaskTurn turn) {
        String reason = Optional.ofNullable(turn.getFinalSummary())
                .filter(summary -> !summary.isBlank())
                .orElse("未知原因");
        Map<String, String> variables = variables(task, turn);
        variables.put("reason", reason);
        messageCenter.send(NotificationEvent.TURN_HANGING, variables);
    }

    public void notifyTaskSucceeded(Task task, TaskTurn turn, String resultSummary) {
        String summary = Optional.ofNullable(resultSummary)
                .filter(value -> !value.isBlank())
                .orElse("任务已完成，暂无摘要。");
        Map<String, String> variables = variables(task, turn);
        variables.put("resultSummary", summary);
        messageCenter.send(NotificationEvent.TASK_SUCCEEDED, variables);
    }

    private Map<String, String> variables(Task task, TaskTurn turn) {
        Task safeTask = Objects.requireNonNull(task, "task must not be null");
        TaskTurn safeTurn = Objects.requireNonNull(turn, "turn must not be null");
        Map<String, String> variables = new LinkedHashMap<>();
        variables.put("taskTitle", readableTaskTitle(safeTask));
        variables.put("turnNo", String.valueOf(safeTurn.getTurnNo()));
        return variables;
    }

    private String readableTaskTitle(Task task) {
        String title = task.getTitle();
        if (title == null || title.isBlank()) {
            return "未命名任务";
        }
        return title;
    }
}
