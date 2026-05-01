package com.chenhaonee.agents.claudecode.agents;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcess;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcessBuilder;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcessManager;
import com.chenhaonee.agents.claudecode.prompt.ClaudeCodePrompts;
import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;
import com.chenhaonee.agents.claudecode.stream.StreamJsonParser;
import com.chenhaonee.agents.claudecode.task.TaskExecutionObservation;
import com.chenhaonee.agents.claudecode.workspace.WorkspaceManager;
import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.capability.CoordinationApi;
import com.chenhaonee.agents.connect.capability.SessionApi;
import com.chenhaonee.agents.connect.capability.TurnEndReport;
import com.chenhaonee.agents.connect.capability.TurnEndReportRegistry;
import com.chenhaonee.agents.connect.spi.core.TaskAgent;
import com.chenhaonee.agents.connect.spi.model.task.TaskPrepareResult;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnRequest;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnResult;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * 基于 Claude Code CLI 的 TaskAgent 实现。
 *
 * <p>生命周期：
 * <ul>
 *   <li>prepare：prepareHome + ensureTaskWorkspace + writeTaskMd</li>
 *   <li>runTurn：writeClaudeMdForTask（每轮刷新）→ spawn claude → 终结时复位 CLAUDE.md</li>
 * </ul>
 *
 * <p>终态判定顺序：hasPendingQuestions → turnEndReport → 无上报时注入 follow-up 重试（最多 MAX_REPORT_RETRIES 次）→ hanging。
 */
@Component
@RequiredArgsConstructor
public class ClaudeCodeTaskAgent implements TaskAgent {

    private static final Logger log = LoggerFactory.getLogger(ClaudeCodeTaskAgent.class);

    /** 未调用 reportTurnEnd 时最多重试次数（不含首次）。 */
    private static final int MAX_REPORT_RETRIES = 3;

    private static final String REPORT_RETRY_PROMPT =
            "你尚未调用 reportTurnEnd。请选择：\n" +
            "- 如果本轮任务已完成，调用 reportTurnEnd 上报结果；\n" +
            "- 如果还有工作需要继续，直接继续执行后再调用 reportTurnEnd。";

    private final ClaudeCodeProperties properties;
    private final AgentConfigApi agentConfigApi;
    private final ClaudeCodeProcessBuilder processBuilder;
    private final ClaudeCodeProcessManager processManager;
    private final WorkspaceManager workspaceManager;
    private final ClaudeCodePrompts claudeCodePrompts;
    private final SessionApi sessionApi;
    private final CoordinationApi coordinationApi;
    private final TurnEndReportRegistry turnEndReportRegistry;

    private final StreamJsonParser streamJsonParser = new StreamJsonParser();

    @Override
    public AgentProvider supportedType() {
        return AgentProvider.CLAUDE_CODE;
    }

    @Override
    public TaskPrepareResult prepare(String agentCode, String taskCode) {
        try {
            workspaceManager.prepareHome(agentCode);
            workspaceManager.ensureTaskWorkspace(agentCode, taskCode);
            workspaceManager.writeTaskMd(agentCode, taskCode);

            log.info("Task workspace prepared: agent={}, task={}", agentCode, taskCode);
            return TaskPrepareResult.finish();
        } catch (Exception e) {
            log.error("Failed to prepare task workspace: agent={}, task={}", agentCode, taskCode, e);
            return TaskPrepareResult.failed("任务环境准备失败: " + e.getMessage());
        }
    }

    @Override
    public TaskTurnResult runTurn(String agentCode, TaskTurnRequest request) {
        Map<String, String> providerConfig = agentConfigApi.getProviderConfig(agentCode);

        String previousSessionId = sessionApi.getProviderSessionId(
                SessionRelationTargetType.TASK_TURN, request.turnCode(), AgentProvider.CLAUDE_CODE);
        boolean isResume = previousSessionId != null && !previousSessionId.isBlank();
        String model = providerConfig.getOrDefault("model", properties.getDefaultModel());
        int maxTurns = Integer.parseInt(providerConfig.getOrDefault("maxTurns",
                String.valueOf(properties.getTaskMaxTurns())));

        Path agentHome = workspaceManager.getAgentHome(agentCode);
        Path mcpFile = agentHome.resolve("mcp-config.json");

        String currentSessionId = previousSessionId;
        boolean currentIsResume = isResume;
        TaskExecutionObservation lastObservation = new TaskExecutionObservation();

        try {
            workspaceManager.writeClaudeMdForTask(agentCode, request.taskCode(), request.turnCode());
            String systemPrompt = claudeCodePrompts.renderTaskSystemPrompt(request.taskCode());

            for (int attempt = 0; attempt <= MAX_REPORT_RETRIES; attempt++) {
                String prompt = attempt == 0
                        ? claudeCodePrompts.renderTurnUserPrompt(
                                request.taskCode(), request.turnCode(), currentIsResume ? currentSessionId : null)
                        : REPORT_RETRY_PROMPT;

                String processId = "task-" + request.taskCode() + "-" + request.turnCode() + "-" + attempt;
                TaskExecutionObservation observation = new TaskExecutionObservation();
                ClaudeCodeProcess process = null;
                boolean registered = false;

                try {
                    List<String> command = processBuilder.buildTaskProcess(
                            agentHome.toString(), currentIsResume, currentSessionId,
                            model, maxTurns, mcpFile.toString(), systemPrompt);
                    process = new ClaudeCodeProcess(command, agentHome.toString());
                    process.start(prompt);
                    processManager.registerTask(processId, process);
                    registered = true;

                    process.streamLines()
                            .doOnNext(line -> {
                                observation.recordLine(line);
                                StreamJsonEvent event = streamJsonParser.parseLine(line);
                                observation.recordEvent(event);
                            })
                            .toStream()
                            .toList();

                    boolean completed = process.waitFor(properties.getTaskTimeoutSeconds());
                    int exitCode = process.exitCode();
                    lastObservation = observation;

                    List<String> stderr = process.getStderr();
                    if (!stderr.isEmpty()) {
                        log.debug("Claude Code stderr for {}: {}", processId, stderr);
                    }

                    String newSessionId = observation.capturedSessionId();
                    syncProviderSession(request.turnCode(), newSessionId);
                    if (newSessionId != null && !newSessionId.isBlank()) {
                        currentSessionId = newSessionId;
                        currentIsResume = true;
                    }

                    if (!completed || exitCode != 0) {
                        String reason = !completed ? "process timed out" : "exit code " + exitCode;
                        log.warn("Task {} turn {} failed (attempt {}): {}",
                                request.taskCode(), request.turnCode(), attempt + 1, reason);
                        return TaskTurnResult.hanging("任务执行失败: " + reason, observation.rawTranscript());
                    }

                } finally {
                    cleanupProcess(processId, process, registered);
                }

                if (coordinationApi.hasPendingQuestions(request.turnCode())) {
                    log.info("Task {} turn {} stopped for coordination", request.taskCode(), request.turnCode());
                    return TaskTurnResult.waitCoordination();
                }

                TurnEndReport report = turnEndReportRegistry.consume(request.turnCode());
                if (report != null) {
                    log.info("Task {} turn {} terminated: progress={}", request.taskCode(), request.turnCode(), report.progress());
                    return TaskTurnResult.terminated(report.progress(), report.summary(), report.detail());
                }

                if (attempt < MAX_REPORT_RETRIES) {
                    log.warn("Task {} turn {} attempt {} ended without reportTurnEnd, injecting follow-up ({} retries left)",
                            request.taskCode(), request.turnCode(), attempt + 1, MAX_REPORT_RETRIES - attempt);
                }
            }

            log.warn("Task {} turn {} did not call reportTurnEnd after {} attempts, hanging",
                    request.taskCode(), request.turnCode(), MAX_REPORT_RETRIES + 1);
            return TaskTurnResult.hanging(
                    "agent 未上报 turn 结果（已重试 " + MAX_REPORT_RETRIES + " 次）",
                    lastObservation.rawTranscript());

        } catch (Exception e) {
            log.error("Task {} turn {} execution error", request.taskCode(), request.turnCode(), e);
            return TaskTurnResult.hanging("任务执行异常: " + e.getMessage(), lastObservation.rawTranscript());
        } finally {
            try {
                workspaceManager.writeClaudeMdForChat(agentCode);
            } catch (Exception e) {
                log.warn("Failed to reset CLAUDE.md to chat variant after terminal turn: {}", e.getMessage());
            }
        }
    }

    private void cleanupProcess(String processId, ClaudeCodeProcess process, boolean registered) {
        if (registered) {
            processManager.unregister(processId);
        }
        if (process != null) {
            process.close();
        }
    }

    private void syncProviderSession(String turnCode, String providerSessionId) {
        if (providerSessionId == null || providerSessionId.isBlank()) {
            return;
        }
        String currentProviderSessionId = sessionApi.getProviderSessionId(
                SessionRelationTargetType.TASK_TURN, turnCode, AgentProvider.CLAUDE_CODE);
        if (currentProviderSessionId == null || currentProviderSessionId.isBlank()) {
            sessionApi.bind(SessionRelationTargetType.TASK_TURN, turnCode,
                    AgentProvider.CLAUDE_CODE, providerSessionId);
            return;
        }
        if (!currentProviderSessionId.equals(providerSessionId)) {
            sessionApi.rebind(SessionRelationTargetType.TASK_TURN, turnCode,
                    AgentProvider.CLAUDE_CODE, providerSessionId);
        }
    }
}
