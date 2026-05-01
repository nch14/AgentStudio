package com.chenhaonee.agents.app.application.task;

import com.chenhaonee.agents.domain.notify.service.MessageCenter;
import com.chenhaonee.agents.domain.task.model.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * task 相关通知发送服务。构造通知文案并委托给 MessageCenter 处理。
 */
@Service
public class TaskNotificationService {

    private final MessageCenter messageCenter;

    @Value("${app.notify.config-code.turn-hanging:turn_hanging}")
    private String turnHangingConfigCode;

    @Value("${app.notify.config-code.task-succeeded:task_succeeded}")
    private String taskSucceededConfigCode;

    public TaskNotificationService(MessageCenter messageCenter) {
        this.messageCenter = messageCenter;
    }

    public void notifyTaskWaiting(Task task) {
        messageCenter.send(turnHangingConfigCode,
                "任务等待处理: " + task.getTitle(),
                "Task " + task.getCode() + " 正等待 Boss 处理协同事项");
    }

    public void notifyTaskSucceeded(Task task, String resultSummary) {
        messageCenter.send(taskSucceededConfigCode,
                "任务已完成: " + task.getTitle(),
                "Task " + task.getCode() + " 已成功完成，摘要：" + resultSummary);
    }
}
