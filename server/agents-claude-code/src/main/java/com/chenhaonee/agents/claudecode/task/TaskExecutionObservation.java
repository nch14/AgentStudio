package com.chenhaonee.agents.claudecode.task;

import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;

/**
 * 记录一次 Claude Code task 执行的关键观察结果。
 * 仅保留 raw transcript 与 sessionId 捕获；终态判定改由 reportTurnEnd 工具上报提供。
 */
public class TaskExecutionObservation {

    private final StringBuilder rawTranscript = new StringBuilder();
    private String capturedSessionId;

    public void recordLine(String line) {
        if (line != null && !line.isBlank()) {
            rawTranscript.append(line).append("\n");
        }
    }

    public void recordEvent(StreamJsonEvent event) {
        if (event == null) {
            return;
        }
        if ("result".equals(event.type()) && event.session_id() != null && !event.session_id().isBlank()) {
            capturedSessionId = event.session_id().trim();
        }
    }

    public String capturedSessionId() {
        return capturedSessionId;
    }

    public String rawTranscript() {
        return rawTranscript.toString().trim();
    }
}
