package com.chenhaonee.agents.app.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/**
 * 验证 application.yml 中关键配置不会互相覆盖。
 */
class ApplicationYamlConfigTest {

    @Test
    void shouldKeepSingleSpringRootAndBothMailAndMcpSettings() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"));

        long springRootCount = yaml.lines()
                .filter(line -> "spring:".equals(line.trim()))
                .count();

        assertEquals(1L, springRootCount);
        assertTrue(yaml.contains("  mail:\n"));
        assertTrue(yaml.contains("  ai:\n"));
        assertTrue(yaml.contains("      server:\n        enabled: true"));
    }
}
