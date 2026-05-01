package com.chenhaonee.agents.domain.task.factory;

import com.chenhaonee.agents.common.validator.ValidationUtils;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskSource;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import org.springframework.stereotype.Component;

/**
 * 任务工厂。
 */
@Component
public class TaskFactory {

    public Task createUserTask(String title, String agentCode, String content, String sourceRef) {
        Task task = create(title, agentCode, content, TaskSource.USER_CREATE);
        task.setSourceRef(sourceRef);
        return task;
    }

    public Task createScheduledTask(String title, String agentCode, String content, String sourceRef) {
        Task task = create(title, agentCode, content, TaskSource.SCHEDULED_CREATE);
        task.setSourceRef(sourceRef);
        return task;
    }

    private Task create(String title, String agentCode, String content, TaskSource source) {
        Task task = new Task();
        task.setTitle(ValidationUtils.requireText(title, "title"));
        task.setAgentCode(ValidationUtils.requireText(agentCode, "agentCode"));
        task.setContent(ValidationUtils.requireText(content, "content"));
        task.setSource(source);
        task.setStatus(TaskStatus.CREATED);
        return task;
    }
}
