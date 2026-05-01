package com.chenhaonee.agents.claudecode.task;

import com.chenhaonee.agents.claudecode.stream.StreamJsonEvent;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskExecutionObservationTest {

    @Test
    void shouldCaptureSessionIdFromResultEvent() {
        TaskExecutionObservation observation = new TaskExecutionObservation();

        observation.recordEvent(new StreamJsonEvent(
                "result", "success", null, null, null, "done",
                null, null, "session-123", null, null, null));

        assertEquals("session-123", observation.capturedSessionId());
    }

    @Test
    void shouldNotCaptureSessionIdFromNonResultEvent() {
        TaskExecutionObservation observation = new TaskExecutionObservation();

        observation.recordEvent(new StreamJsonEvent(
                "assistant", "text", "some text", null, null, null,
                null, null, "fake-session", null, null, null));

        assertNull(observation.capturedSessionId());
    }

    @Test
    void shouldAccumulateRawTranscript() {
        TaskExecutionObservation observation = new TaskExecutionObservation();

        observation.recordLine("{\"type\":\"assistant\",\"content\":\"hello\"}");
        observation.recordLine("{\"type\":\"result\",\"subtype\":\"success\"}");

        String transcript = observation.rawTranscript();
        assertTrue(transcript.contains("\"type\":\"assistant\""));
        assertTrue(transcript.contains("\"type\":\"result\""));
    }

    @Test
    void shouldIgnoreNullOrBlankLines() {
        TaskExecutionObservation observation = new TaskExecutionObservation();

        observation.recordLine(null);
        observation.recordLine("   ");
        observation.recordLine("valid");

        assertEquals("valid", observation.rawTranscript());
    }
}
