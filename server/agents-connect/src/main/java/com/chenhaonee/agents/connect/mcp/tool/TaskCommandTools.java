package com.chenhaonee.agents.connect.mcp.tool;

import com.chenhaonee.agents.connect.capability.TaskCommandApi;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 任务命令相关 MCP tools。
 */
@Component
public class TaskCommandTools {

    private final TaskCommandApi taskCommandApi;

    public TaskCommandTools(TaskCommandApi taskCommandApi) {
        this.taskCommandApi = taskCommandApi;
    }

    @Tool(name = "task_create", description = "创建一个新的任务")
    public String createTask(String title, @ToolParam(description = "必填项，任务的具体内容/目标") String content, String agentCode) {
        return taskCommandApi.createTask(title, content, agentCode);
    }

    @Tool(name = "task_retry", description = "重试指定任务")
    public void retryTask(String taskCode) {
        taskCommandApi.retryTask(taskCode);
    }

    @Tool(name = "task_cancel", description = "取消指定任务")
    public void cancelTask(String taskCode, String reason) {
        taskCommandApi.cancelTask(taskCode, reason);
    }

    @Tool(name = "task_get_detail", description = "获取指定任务的简要详情")
    public TaskCommandApi.TaskDetail getTaskDetail(String taskCode) {
        return taskCommandApi.getTaskDetail(taskCode);
    }
}
