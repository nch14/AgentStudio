package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskTurn;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * task 相关通知发送服务。构造通知文案并委托给 MessageCenter 处理。
 */
@Service
public class TaskNotificationService {

    private final MessageCenter messageCenter;

    @Value("${app.notify.config-code.turn-waiting:questions_raised}")
    private String turnWaitingConfigCode;

    @Value("${app.notify.config-code.turn-hanging:turn_hanging}")
    private String turnHangingConfigCode;

    @Value("${app.notify.config-code.task-succeeded:task_succeeded}")
    private String taskSucceededConfigCode;

    public TaskNotificationService(MessageCenter messageCenter) {
        this.messageCenter = messageCenter;
    }

    public void notifyTaskWaiting(Task task) {
        messageCenter.send(turnWaitingConfigCode,
                "任务等待输入: " + task.getTitle(),
                "Task " + task.getCode() + " 正等待用户输入或协同处理");
    }

    public void notifyTaskHanging(Task task, TaskTurn turn) {
        String reason = Optional.ofNullable(turn.getFinalSummary())
                .filter(summary -> !summary.isBlank())
                .orElse("未知原因");
        messageCenter.send(turnHangingConfigCode,
                "任务执行异常: " + task.getTitle(),
                "Task " + task.getCode() + " 的 Turn " + turn.getCode()
                        + " 进入 HANGING 状态，请检查执行链路。原因：" + reason);
    }

    public void notifyTaskSucceeded(Task task, String resultSummary) {
        messageCenter.send(taskSucceededConfigCode,
                "任务已完成: " + task.getTitle(),
                "Task " + task.getCode() + " 已成功完成，摘要：" + resultSummary);
    }
}
