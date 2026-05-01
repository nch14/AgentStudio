package com.chenhaonee.agents.connect.mcp.tool;

import com.chenhaonee.agents.connect.capability.TurnEndReport;
import com.chenhaonee.agents.connect.capability.TurnEndReportRegistry;
import com.chenhaonee.agents.connect.capability.impl.DefaultTurnEndReportRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TurnReportToolsTest {

    private TurnEndReportRegistry registry;
    private TurnReportTools tools;

    @BeforeEach
    void setUp() {
        registry = new DefaultTurnEndReportRegistry();
        tools = new TurnReportTools(registry);
    }

    @Test
    void shouldWriteReportToRegistry() {
        tools.reportTurnEnd("turn-1", 60, "任务完成", "detail");

        TurnEndReport report = registry.consume("turn-1");
        assertNotNull(report);
        assertEquals(60, report.progress());
        assertEquals("任务完成", report.summary());
        assertEquals("detail", report.detail());
    }

    @Test
    void shouldWriteReportWithoutOptionalFields() {
        tools.reportTurnEnd("turn-2", null, "简单结论", null);

        TurnEndReport report = registry.consume("turn-2");
        assertNotNull(report);
        assertNull(report.progress());
        assertNull(report.detail());
        assertEquals("简单结论", report.summary());
    }

    @Test
    void shouldReturnOkOnSuccess() {
        String result = tools.reportTurnEnd("turn-3", 50, "done", null);
        assertEquals("ok", result);
    }

    @Test
    void shouldRejectBlankTurnCode() {
        assertThrows(IllegalArgumentException.class,
                () -> tools.reportTurnEnd("", 50, "done", null));
        assertThrows(IllegalArgumentException.class,
                () -> tools.reportTurnEnd(null, 50, "done", null));
    }

    @Test
    void shouldRejectBlankSummary() {
        assertThrows(IllegalArgumentException.class,
                () -> tools.reportTurnEnd("turn-4", 50, "", null));
        assertThrows(IllegalArgumentException.class,
                () -> tools.reportTurnEnd("turn-4", 50, null, null));
    }

    @Test
    void shouldRejectProgressOutOfRange() {
        assertThrows(IllegalArgumentException.class,
                () -> tools.reportTurnEnd("turn-5", -1, "done", null));
        assertThrows(IllegalArgumentException.class,
                () -> tools.reportTurnEnd("turn-5", 101, "done", null));
    }

    @Test
    void shouldConsumeRemovesEntry() {
        tools.reportTurnEnd("turn-6", 70, "done", null);
        registry.consume("turn-6");

        assertNull(registry.consume("turn-6"));
    }

    @Test
    void shouldOverwriteOnDuplicateReport() {
        tools.reportTurnEnd("turn-7", 30, "first", null);
        tools.reportTurnEnd("turn-7", 90, "second", null);

        TurnEndReport report = registry.consume("turn-7");
        assertEquals(90, report.progress());
        assertEquals("second", report.summary());
    }
}
