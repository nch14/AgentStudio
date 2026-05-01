package com.chenhaonee.agents.claudecode.workspace;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.prompt.ClaudeCodePrompts;
import com.chenhaonee.agents.common.config.AgentWorkspaceProperties;
import com.chenhaonee.agents.domain.task.model.Task;
import com.chenhaonee.agents.domain.task.model.TaskStatus;
import com.chenhaonee.agents.domain.task.repository.TaskRepository;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 管理 Claude Code 工作目录的生命周期。
 *
 * <p>目录布局：
 * <pre>
 * {workspace-path}/
 *   {agentCode}/                    ← agent-home（每个 agent 独立）
 *     CLAUDE.md                     ← 框架生成，不回写
 *     mcp-config.json               ← 框架生成，不回写
 *     soul.md
 *     owner.md
 *     tool.md
 *     memory/
 *       overview.md
 *     workspace/
 *       {taskCode}/                 ← task 模式工作区
 *         task.md                   ← 框架生成，不回写
 *         {agent 产出文件}
 * </pre>
 */
@Component
@RequiredArgsConstructor
public class WorkspaceManager {

    private static final Logger log = LoggerFactory.getLogger(WorkspaceManager.class);

    private static final String WORKSPACE_DIR = "workspace";

    private final ClaudeCodeProperties properties;
    private final AgentWorkspaceProperties workspaceProperties;
    private final TaskRepository taskRepository;
    private final ClaudeCodePrompts claudeCodePrompts;

    /**
     * 启动时：仅确保 workspace root 存在。
     */
    @PostConstruct
    public void initWorkspace() throws IOException {
        Path root = Paths.get(workspaceProperties.getWorkspacePath());
        if (!Files.exists(root)) {
            Files.createDirectories(root);
            log.info("Created workspace root: {}", root);
        }
    }

    /**
     * 返回 agent home 路径。
     */
    public Path getAgentHome(String agentCode) {
        return Paths.get(workspaceProperties.getWorkspacePath(), agentCode);
    }

    /**
     * Bootstrap：确保本地 agent home 目录和 CLAUDE.md 存在。
     * 本地文件系统即真相，不再从 OSS 下载。
     */
    public void prepareHome(String agentCode) throws IOException {
        Path agentHome = getAgentHome(agentCode);
        Files.createDirectories(agentHome);
        ensureClaudeMd(agentCode);
    }

    /**
     * 确保 task 工作区目录存在。
     */
    public void ensureTaskWorkspace(String agentCode, String taskCode) throws IOException {
        Path agentHome = getAgentHome(agentCode);
        Path taskDir = agentHome.resolve(WORKSPACE_DIR).resolve(taskCode);
        Files.createDirectories(taskDir);
    }

    /**
     * 写入 CLAUDE.md（chat variant）。
     * 条件写：仅在缺失或当前为已终结的 task-flavor 时写入。
     */
    public void ensureClaudeMd(String agentCode) throws IOException {
        Path agentHome = getAgentHome(agentCode);
        if (!Files.exists(agentHome)) {
            return;
        }
        Path claudeMd = agentHome.resolve("CLAUDE.md");

        if (!Files.exists(claudeMd)) {
            writeClaudeMdContent(claudeMd, claudeCodePrompts.renderClaudeMdChat(agentCode));
            return;
        }

        // 检查是否是 task-flavor（含 "## Current Task"）且任务已终结
        String existing = Files.readString(claudeMd);
        if (existing.contains("## Current Task")) {
            String taskCode = extractTaskCode(existing);
            if (taskCode != null && isTaskTerminated(taskCode)) {
                // 任务已终结，复位为 chat-flavor
                writeClaudeMdContent(claudeMd, claudeCodePrompts.renderClaudeMdChat(agentCode));
            }
            // 任务仍活跃：保留，让 chat 看到 Current Task 段落
        }
        // 已是 chat-flavor：不动
    }

    /**
     * 强制写入 CLAUDE.md（task variant），每轮 runTurn 开始时调用。
     */
    public void writeClaudeMdForTask(String agentCode, String taskCode, String turnCode) throws IOException {
        Path claudeMd = getAgentHome(agentCode).resolve("CLAUDE.md");
        writeClaudeMdContent(claudeMd, claudeCodePrompts.renderClaudeMdTask(agentCode, taskCode, turnCode));
    }

    /**
     * 强制复位 CLAUDE.md 为 chat variant（task 终结后调用）。
     */
    public void writeClaudeMdForChat(String agentCode) throws IOException {
        Path claudeMd = getAgentHome(agentCode).resolve("CLAUDE.md");
        writeClaudeMdContent(claudeMd, claudeCodePrompts.renderClaudeMdChat(agentCode));
    }

    /**
     * 写入 task.md（从 Task.content 生成）。
     */
    public void writeTaskMd(String agentCode, String taskCode) throws IOException {
        Path taskDir = getAgentHome(agentCode).resolve(WORKSPACE_DIR).resolve(taskCode);
        Path taskMd = taskDir.resolve("task.md");
        Files.createDirectories(taskDir);
        Task task = taskRepository.findByCode(taskCode)
                .orElseThrow(() -> new IllegalArgumentException("Task not found: " + taskCode));
        String content = claudeCodePrompts.renderTaskMd(taskCode, task.getTitle(), task.getContent());
        Files.writeString(taskMd, content);
    }

    /**
     * 每天凌晨清理过期 task 工作区目录（workspace/<taskCode>/）。
     */
    public void cleanupCompletedTasks() {
        Path root = Paths.get(workspaceProperties.getWorkspacePath());
        if (!Files.exists(root)) {
            return;
        }

        long retentionMs = properties.getTaskRetentionDays() * 24L * 3600L * 1000L;
        long now = System.currentTimeMillis();

        try (Stream<Path> agentDirs = Files.list(root)) {
            agentDirs.filter(Files::isDirectory).forEach(agentHome -> {
                Path workspaceDir = agentHome.resolve(WORKSPACE_DIR);
                if (!Files.isDirectory(workspaceDir)) {
                    return;
                }
                try (Stream<Path> taskDirs = Files.list(workspaceDir)) {
                    taskDirs.filter(Files::isDirectory).forEach(taskDir -> {
                        try {
                            long modifiedMs = Files.getLastModifiedTime(taskDir).toMillis();
                            if (now - modifiedMs > retentionMs && canCleanupTaskWorkspace(taskDir)) {
                                log.info("Cleaning up expired task workspace: {}", taskDir);
                                deleteRecursively(taskDir);
                            }
                        } catch (IOException e) {
                            log.warn("Failed to check task dir {}: {}", taskDir, e.getMessage());
                        }
                    });
                } catch (IOException e) {
                    log.warn("Failed to list workspace dir {}: {}", workspaceDir, e.getMessage());
                }
            });
        } catch (IOException e) {
            log.error("Failed to scan workspace root for cleanup: {}", e.getMessage());
        }
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    boolean canCleanupTaskWorkspace(Path taskDir) {
        Path taskCodePath = taskDir.getFileName();
        if (taskCodePath == null) {
            return false;
        }
        return taskRepository.findByCode(taskCodePath.toString())
                .map(task -> isTerminalStatus(task.getStatus()))
                .orElse(false);
    }

    private void writeClaudeMdContent(Path claudeMd, String content) throws IOException {
        Files.writeString(claudeMd, content);
    }

    /**
     * 从 CLAUDE.md task-flavor 内容中提取 taskCode。
     */
    private String extractTaskCode(String content) {
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- Task code:")) {
                return trimmed.substring("- Task code:".length()).trim();
            }
        }
        return null;
    }

    private boolean isTaskTerminated(String taskCode) {
        return taskRepository.findByCode(taskCode)
                .map(task -> isTerminalStatus(task.getStatus()))
                .orElse(true);
    }

    private boolean isTerminalStatus(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED || status == TaskStatus.CANCELLED;
    }

    private void deleteRecursively(Path path) throws IOException {
        try (Stream<Path> stream = Files.walk(path)) {
            stream.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    log.warn("Failed to delete {}: {}", p, e.getMessage());
                }
            });
        }
    }
}
