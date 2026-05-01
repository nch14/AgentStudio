package com.chenhaonee.agents.app.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.chenhaonee.agents.AgentsApplication;
import com.chenhaonee.agents.domain.profile.model.OwnerProfile;
import com.chenhaonee.agents.domain.profile.repository.OwnerProfileRepository;
import com.chenhaonee.agents.domain.task.factory.TaskFactory;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = AgentsApplication.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskFactory taskFactory;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private OwnerProfileRepository ownerProfileRepository;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        ownerProfileRepository.deleteAll();

        OwnerProfile ownerProfile = new OwnerProfile();
        ownerProfile.updateProfile("测试用户", null, "debug-device-key", "本地调试");
        ownerProfile.updatePreferences("Asia/Shanghai", "zh-CN");
        ownerProfileRepository.save(ownerProfile);
    }

    private Task createTask(String title) {
        Task task = taskFactory.createUserTask(title, "test-agent", "test content", "test-owner");
        return taskRepository.save(task);
    }

    @Test
    void shouldCreateTask() throws Exception {
        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.chenhaonee.agents.app.interfaces.http.task.dto.TaskCreateRequest(
                                        "测试任务", "test-agent", "任务描述"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("测试任务"))
                .andExpect(jsonPath("$.data.agentCode").value("test-agent"))
                .andExpect(jsonPath("$.data.source").value("USER_CREATE"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void shouldGetTaskDetail() throws Exception {
        Task task = createTask("查看详情任务");

        mockMvc.perform(get("/api/tasks/{taskCode}", task.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.taskCode").value(task.getCode()))
                .andExpect(jsonPath("$.data.title").value("查看详情任务"))
                .andExpect(jsonPath("$.data.status").value("CREATED"));
    }

    @Test
    void shouldGetTaskDetailWhenCurrentTurnIsMissing() throws Exception {
        Task task = createTask("缺失当前回合的任务");
        task.startTurn("missing-turn-code");
        taskRepository.save(task);

        mockMvc.perform(get("/api/tasks/{taskCode}", task.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.taskCode").value(task.getCode()))
                .andExpect(jsonPath("$.data.currentTurnCode").value("missing-turn-code"));
    }

    @Test
    void shouldListTasks() throws Exception {
        createTask("任务一");
        createTask("任务二");

        mockMvc.perform(get("/api/tasks")
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.total").value(2));
    }

    @Test
    void shouldFilterTasksBySource() throws Exception {
        createTask("用户任务");

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                .param("size", "10")
                .param("source", "USER_CREATE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void shouldFilterTasksByStatus() throws Exception {
        createTask("已创建任务");

        mockMvc.perform(get("/api/tasks")
                        .param("page", "0")
                .param("size", "10")
                .param("status", "CREATED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));

        mockMvc.perform(get("/api/tasks")
                .param("page", "0")
                .param("size", "10")
                .param("status", "SUCCEEDED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void shouldUpdateTask() throws Exception {
        Task task = createTask("原任务");

        mockMvc.perform(put("/api/tasks/{taskCode}", task.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.chenhaonee.agents.app.interfaces.http.task.dto.TaskUpdateRequest(
                                        "新标题", "新描述"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.title").value("新标题"));
    }

    @Test
    void shouldCancelTask() throws Exception {
        Task task = createTask("待取消任务");

        mockMvc.perform(post("/api/tasks/{taskCode}/cancel", task.getCode())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new com.chenhaonee.agents.app.interfaces.http.task.dto.TaskCancelRequest("不需要了"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void shouldDeleteTask() throws Exception {
        Task task = createTask("待删除任务");

        mockMvc.perform(post("/api/tasks/{taskCode}/delete", task.getCode()))
                .andExpect(status().isOk());

        // Deleted task should not be found (filtered by valid=true)
        mockMvc.perform(get("/api/tasks/{taskCode}", task.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRetrySucceededTask() throws Exception {
        Task task = createTask("已完成任务");
        task.markRunning();
        task.succeed("任务已完成");
        taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/{taskCode}/retry", task.getCode()))
                .andExpect(status().isOk());

        Task reloaded = taskRepository.findByCode(task.getCode()).orElseThrow();
        assertEquals(TaskStatus.CREATED, reloaded.getStatus());
        assertNotNull(reloaded.getCurrentTurnCode());
    }

    @Test
    void shouldRetryCancelledTask() throws Exception {
        Task task = createTask("已取消任务");
        task.cancel("取消了");
        taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/{taskCode}/retry", task.getCode()))
                .andExpect(status().isOk());

        Task reloaded = taskRepository.findByCode(task.getCode()).orElseThrow();
        assertEquals(TaskStatus.CREATED, reloaded.getStatus());
    }

    @Test
    void shouldReturn404ForNonExistentTask() throws Exception {
        mockMvc.perform(get("/api/tasks/{taskCode}", "non-existent"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void shouldRollbackSucceededTask() throws Exception {
        Task task = createTask("已完成任务-回滚");
        task.markRunning();
        task.succeed("任务已完成");
        taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/{taskCode}/rollback", task.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));

        Task reloaded = taskRepository.findByCode(task.getCode()).orElseThrow();
        assertEquals(TaskStatus.RUNNING, reloaded.getStatus());
        assertEquals(null, reloaded.getFinishedAt());
        assertEquals(null, reloaded.getResultSummary());
    }

    @Test
    void shouldRollbackCancelledTask() throws Exception {
        Task task = createTask("已取消任务-回滚");
        task.cancel("取消了");
        taskRepository.save(task);

        mockMvc.perform(post("/api/tasks/{taskCode}/rollback", task.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("RUNNING"));

        Task reloaded = taskRepository.findByCode(task.getCode()).orElseThrow();
        assertEquals(TaskStatus.RUNNING, reloaded.getStatus());
        assertEquals(null, reloaded.getFinishedAt());
    }

    @Test
    void shouldFailRollbackForNonTerminalTask() throws Exception {
        Task task = createTask("未完成任务-回滚失败");

        mockMvc.perform(post("/api/tasks/{taskCode}/rollback", task.getCode()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void shouldReturnNoContentForActiveQuestionsWhenNone() throws Exception {
        Task task = createTask("无问题任务");

        mockMvc.perform(get("/api/tasks/{taskCode}/questions/active", task.getCode()))
                .andExpect(status().isNoContent());
    }
}
