package com.chenhaonee.agents.claudecode.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.prompt.ClaudeCodePrompts;
import com.chenhaonee.agents.common.config.AgentWorkspaceProperties;
import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.spi.context.AgentProfile;
import com.chenhaonee.agents.connect.spi.context.OwnerContext;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class WorkspaceManagerTest {

    @TempDir
    Path tempDir;

    private ClaudeCodeProperties properties;
    private AgentWorkspaceProperties workspaceProperties;
    private TaskRepository taskRepository;
    private AgentConfigApi agentConfigApi;
    private ClaudeCodePrompts claudeCodePrompts;

    @BeforeEach
    void setUp() {
        properties = new ClaudeCodeProperties();
        workspaceProperties = new AgentWorkspaceProperties();
        workspaceProperties.setWorkspacePath(tempDir.toString());
        taskRepository = mockTaskRepository(Map.of());

        // Mock AgentConfigApi for ClaudeCodePrompts
        agentConfigApi = mock(AgentConfigApi.class);
        AgentProfile profile = new AgentProfile("TestAgent", "Test Responsibility", Map.of());
        OwnerContext owner = new OwnerContext("Test User", null, "Asia/Shanghai", "zh_CN", null, null);
        when(agentConfigApi.getAgentProfile("agent-01")).thenReturn(profile);
        when(agentConfigApi.getAgentProfile("agent-02")).thenReturn(profile);
        when(agentConfigApi.getAgentProfile("agent-03")).thenReturn(profile);
        when(agentConfigApi.getOwnerContext()).thenReturn(owner);

        claudeCodePrompts = new ClaudeCodePrompts(agentConfigApi);
    }

    /**
     * canCleanupTaskWorkspace 应当只允许清理已终结（SUCCEEDED/CANCELLED）的任务目录。
     */
    @Test
    void shouldAllowCleanupForTerminalTaskOnly() {
        TaskRepository repo = stubTaskRepository(Map.of(
                "task-success", taskWithStatus(TaskStatus.SUCCEEDED),
                "task-cancelled", taskWithStatus(TaskStatus.CANCELLED),
                "task-waiting", taskWithStatus(TaskStatus.RUNNING)));

        WorkspaceManager workspaceManager = new WorkspaceManager(
                properties, workspaceProperties, repo, claudeCodePrompts);

        assertTrue(workspaceManager.canCleanupTaskWorkspace(Path.of("/data/agents/agent-01/workspace/task-success")));
        assertTrue(workspaceManager.canCleanupTaskWorkspace(Path.of("/data/agents/agent-01/workspace/task-cancelled")));
        assertFalse(workspaceManager.canCleanupTaskWorkspace(Path.of("/data/agents/agent-01/workspace/task-waiting")));
        assertFalse(workspaceManager.canCleanupTaskWorkspace(Path.of("/data/agents/agent-01/workspace/task-missing")));
    }

    /**
     * canCleanupTaskWorkspace 对路径末尾为 null 的情况应返回 false。
     */
    @Test
    void shouldReturnFalseForNullTaskCodePath() {
        WorkspaceManager workspaceManager = new WorkspaceManager(
                properties, workspaceProperties, stubTaskRepository(Map.of()),
                claudeCodePrompts);

        // Path.of("") 的 getFileName() 返回 null，应被安全处理
        assertFalse(workspaceManager.canCleanupTaskWorkspace(Path.of("")));
    }

    /**
     * prepareHome 应当只创建目录和 CLAUDE.md，不写 mcp-config.json。
     */
    @Test
    void prepareHomeShouldCreateDirectoriesAndClaudeMd() throws Exception {
        String agentCode = "agent-01";

        WorkspaceManager workspaceManager = new WorkspaceManager(
                properties, workspaceProperties, taskRepository, claudeCodePrompts);

        workspaceManager.prepareHome(agentCode);

        Path agentHome = tempDir.resolve(agentCode);
        assertTrue(Files.exists(agentHome));
        assertTrue(Files.isDirectory(agentHome));
        assertTrue(Files.exists(agentHome.resolve("CLAUDE.md")));
    }

    /**
     * prepareHome 对已存在的目录应幂等。
     */
    @Test
    void prepareHomeShouldBeIdempotent() throws Exception {
        String agentCode = "agent-01";
        Path agentHome = tempDir.resolve(agentCode);
        Files.createDirectories(agentHome);
        Files.writeString(agentHome.resolve("CLAUDE.md"), "existing content");

        WorkspaceManager workspaceManager = new WorkspaceManager(
                properties, workspaceProperties, taskRepository, claudeCodePrompts);

        workspaceManager.prepareHome(agentCode);

        // 已存在的 CLAUDE.md 不应被覆盖（ensureClaudeMd 的条件写逻辑）
        assertEquals("existing content", Files.readString(agentHome.resolve("CLAUDE.md")));
    }

    /**
     * ensureTaskWorkspace 仅创建工作目录。
     */
    @Test
    void ensureTaskWorkspaceShouldCreateDirectoryOnly() throws Exception {
        String agentCode = "agent-01";
        String taskCode = "task-001";

        WorkspaceManager workspaceManager = new WorkspaceManager(
                properties, workspaceProperties, taskRepository, claudeCodePrompts);

        workspaceManager.prepareHome(agentCode);
        workspaceManager.ensureTaskWorkspace(agentCode, taskCode);

        Path taskDir = tempDir.resolve(agentCode).resolve("workspace").resolve(taskCode);
        assertTrue(Files.exists(taskDir));
        assertTrue(Files.isDirectory(taskDir));
    }

    /**
     * writeTaskMd 应当从 Task 实体生成内容并覆盖写入。
     */
    @Test
    void writeTaskMdShouldGenerateFromTaskEntity() throws Exception {
        String agentCode = "agent-01";
        String taskCode = "task-001";

        taskRepository = stubTaskRepository(Map.of(
                "task-001", taskWithStatus(TaskStatus.RUNNING)));

        WorkspaceManager workspaceManager = new WorkspaceManager(
                properties, workspaceProperties, taskRepository, claudeCodePrompts);

        workspaceManager.prepareHome(agentCode);
        workspaceManager.writeTaskMd(agentCode, taskCode);

        Path taskMd = tempDir.resolve(agentCode).resolve("workspace").resolve(taskCode).resolve("task.md");
        assertTrue(Files.exists(taskMd));
        String content = Files.readString(taskMd);
        assertTrue(content.contains("# Task\ntask-001"));
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private TaskRepository mockTaskRepository(Map<String, Task> tasks) {
        return stubTaskRepository(tasks);
    }

    private TaskRepository stubTaskRepository(Map<String, Task> tasks) {
        return (TaskRepository) Proxy.newProxyInstance(
                TaskRepository.class.getClassLoader(),
                new Class[]{TaskRepository.class},
                (proxy, method, args) -> {
                    if ("findByCode".equals(method.getName())) {
                        return Optional.ofNullable(tasks.get(args[0]));
                    }
                    throw new UnsupportedOperationException("Unexpected method: " + method.getName());
                });
    }

    private Task taskWithStatus(TaskStatus status) {
        Task task = new Task();
        task.setTitle("Test Task");
        task.setContent("Test content");
        task.setStatus(status);
        return task;
    }
}
