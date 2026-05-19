package com.chenhaonee.agents.claudecode.agents;

import com.chenhaonee.agents.claudecode.ClaudeCodeProperties;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcessBuilder;
import com.chenhaonee.agents.claudecode.process.ClaudeCodeProcessManager;
import com.chenhaonee.agents.claudecode.prompt.ClaudeCodePrompts;
import com.chenhaonee.agents.claudecode.workspace.WorkspaceManager;
import com.chenhaonee.agents.connect.capability.AgentConfigApi;
import com.chenhaonee.agents.connect.capability.CoordinationApi;
import com.chenhaonee.agents.connect.capability.SessionApi;
import com.chenhaonee.agents.connect.capability.TurnEndReport;
import com.chenhaonee.agents.connect.capability.TurnEndReportRegistry;
import com.chenhaonee.agents.connect.spi.model.task.TaskNextAction;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnRequest;
import com.chenhaonee.agents.connect.spi.model.task.TaskTurnResult;
import com.chenhaonee.agents.connect.support.MessagesEventBlockRecorderFactory;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import com.chenhaonee.agents.domain.session.model.MessageProtocolType;
import com.chenhaonee.agents.domain.session.model.SessionRelationTargetType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClaudeCodeTaskAgentTest {

    @TempDir
    Path tempDir;

    @Mock
    private AgentConfigApi agentConfigApi;

    @Mock
    private WorkspaceManager workspaceManager;

    @Mock
    private ClaudeCodePrompts claudeCodePrompts;

    @Mock
    private SessionApi sessionApi;

    @Mock
    private CoordinationApi coordinationApi;

    @Mock
    private TurnEndReportRegistry turnEndReportRegistry;

    @Mock
    private MessagesEventBlockRecorderFactory messagesEventBlockRecorderFactory;

    @Mock
    private MessagesEventBlockRecorderFactory.MessagesEventBlockRecorder messagesEventBlockRecorder;

    @Test
    void shouldTerminateWithReportedProgressWhenReportTurnEndCalled() throws Exception {
        Path argsFile = tempDir.resolve("args-first.txt");
        Path cliScript = createFakeCliScript("cc-session-1", argsFile);
        ClaudeCodeTaskAgent agent = newAgent(cliScript);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of());
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(tempDir);
        when(claudeCodePrompts.renderTaskSystemPrompt("task-1")).thenReturn("system");
        when(claudeCodePrompts.renderTurnUserPrompt("task-1", "turn-1", null)).thenReturn("user");
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.TASK, "task-1", AgentProvider.CLAUDE_CODE))
                .thenReturn(null);
        when(coordinationApi.hasPendingQuestions("turn-1")).thenReturn(false);
        when(turnEndReportRegistry.consume("turn-1"))
                .thenReturn(new TurnEndReport(60, "任务完成", null));

        TaskTurnResult result = agent.runTurn("agent-1", new TaskTurnRequest("task-1", "turn-1", "session-1"));

        assertEquals(TaskNextAction.TERMINATED, result.outcome());
        assertEquals(60, result.progress());
        assertEquals("任务完成", result.resultSummary());
        verify(sessionApi).bind(
                SessionRelationTargetType.TASK, "task-1", AgentProvider.CLAUDE_CODE, "cc-session-1");
        verify(sessionApi, never()).rebind(
                eq(SessionRelationTargetType.TASK), eq("task-1"), eq(AgentProvider.CLAUDE_CODE), eq("cc-session-1"));
    }

    @Test
    void shouldResumeByTurnCodeAndRebindWhenClaudeReturnsNewSessionId() throws Exception {
        Path argsFile = tempDir.resolve("args-resume.txt");
        Path cliScript = createFakeCliScript("cc-session-2", argsFile);
        ClaudeCodeTaskAgent agent = newAgent(cliScript);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of());
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(tempDir);
        when(claudeCodePrompts.renderTaskSystemPrompt("task-1")).thenReturn("system");
        when(claudeCodePrompts.renderTurnUserPrompt("task-1", "turn-1", "old-session")).thenReturn("user");
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.TASK, "task-1", AgentProvider.CLAUDE_CODE))
                .thenReturn("old-session", "old-session");
        when(coordinationApi.hasPendingQuestions("turn-1")).thenReturn(false);
        when(turnEndReportRegistry.consume("turn-1"))
                .thenReturn(new TurnEndReport(80, "恢复后完成", null));

        TaskTurnResult result = agent.runTurn("agent-1", new TaskTurnRequest("task-1", "turn-1", "session-1"));

        assertEquals(TaskNextAction.TERMINATED, result.outcome());
        verify(sessionApi).rebind(
                SessionRelationTargetType.TASK, "task-1", AgentProvider.CLAUDE_CODE, "cc-session-2");
        String args = Files.readString(argsFile);
        assertTrue(args.contains("--resume"));
        assertTrue(args.contains("old-session"));
    }

    @Test
    void shouldHangAfterExhaustingRetriesWhenReportTurnEndNeverCalled() throws Exception {
        Path argsFile = tempDir.resolve("args-retry.txt");
        Path cliScript = createFakeCliScript("cc-session-3", argsFile);
        ClaudeCodeTaskAgent agent = newAgent(cliScript);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of());
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(tempDir);
        when(claudeCodePrompts.renderTaskSystemPrompt("task-1")).thenReturn("system");
        when(claudeCodePrompts.renderTurnUserPrompt(eq("task-1"), eq("turn-1"), any())).thenReturn("user");
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.TASK, "task-1", AgentProvider.CLAUDE_CODE))
                .thenReturn(null);
        when(coordinationApi.hasPendingQuestions("turn-1")).thenReturn(false);
        // consume never returns a report
        when(turnEndReportRegistry.consume("turn-1")).thenReturn(null);

        TaskTurnResult result = agent.runTurn("agent-1", new TaskTurnRequest("task-1", "turn-1", "session-1"));

        assertEquals(TaskNextAction.HANGING, result.outcome());
        assertTrue(result.resultSummary().contains("未上报"));
    }

    @Test
    void shouldWaitCoordinationWhenPendingQuestionsExist() throws Exception {
        Path argsFile = tempDir.resolve("args-coord.txt");
        Path cliScript = createFakeCliScript("cc-session-4", argsFile);
        ClaudeCodeTaskAgent agent = newAgent(cliScript);

        when(agentConfigApi.getProviderConfig("agent-1")).thenReturn(Map.of());
        when(workspaceManager.getAgentHome("agent-1")).thenReturn(tempDir);
        when(claudeCodePrompts.renderTaskSystemPrompt("task-1")).thenReturn("system");
        when(claudeCodePrompts.renderTurnUserPrompt("task-1", "turn-1", null)).thenReturn("user");
        when(sessionApi.getProviderSessionId(
                SessionRelationTargetType.TASK, "task-1", AgentProvider.CLAUDE_CODE))
                .thenReturn(null);
        when(coordinationApi.hasPendingQuestions("turn-1")).thenReturn(true);

        TaskTurnResult result = agent.runTurn("agent-1", new TaskTurnRequest("task-1", "turn-1", "session-1"));

        assertEquals(TaskNextAction.WAIT_COORDINATION, result.outcome());
    }

    private ClaudeCodeTaskAgent newAgent(Path cliScript) {
        ClaudeCodeProperties properties = new ClaudeCodeProperties();
        properties.setCliPath(cliScript.toString());
        properties.setTaskTimeoutSeconds(5);
        ClaudeCodeProcessBuilder processBuilder = new ClaudeCodeProcessBuilder(properties);
        ClaudeCodeProcessManager processManager = new ClaudeCodeProcessManager(properties);
        when(messagesEventBlockRecorderFactory.create(
                eq("session-1"), eq("turn-1"), eq(MessageProtocolType.ANTHROPIC_MESSAGES)))
                .thenReturn(messagesEventBlockRecorder);
        return new ClaudeCodeTaskAgent(
                properties,
                agentConfigApi,
                processBuilder,
                processManager,
                workspaceManager,
                claudeCodePrompts,
                sessionApi,
                coordinationApi,
                turnEndReportRegistry,
                messagesEventBlockRecorderFactory
        );
    }

    private Path createFakeCliScript(String sessionId, Path argsFile) throws IOException {
        Path script = tempDir.resolve("fake-claude-" + sessionId + ".sh");
        String content = """
                #!/bin/sh
                printf '%%s\n' "$@" > '%s'
                cat >/dev/null
                printf '%%s\n' '%s'
                """.formatted(
                argsFile.toString(),
                "{\"type\":\"result\",\"subtype\":\"success\",\"result\":\"任务完成\",\"session_id\":\"" + sessionId + "\"}"
        );
        Files.writeString(script, content);
        script.toFile().setExecutable(true);
        return script;
    }
}
