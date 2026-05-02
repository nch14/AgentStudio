package com.chenhaonee.agents.claudecode.mcp;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

/**
 * 生成 Claude Code 的 MCP 配置文件 (mcp-config.json)。
 */
@Component
public class McpConfigGenerator {

    /**
     * 生成 MCP 配置，写入指定目录。
     * @return mcp-config.json 文件路径
     */
    public Path generate(Path workDir, String mcpServerUrl) throws IOException {
        Path mcpFile = workDir.resolve("mcp-config.json");
        Map<String, Object> config = buildMcpConfig(mcpServerUrl);
        Files.writeString(mcpFile, JSON.toJSONString(config, JSONWriter.Feature.PrettyFormat));
        return mcpFile;
    }

    /**
     * 仅在 mcp-config.json 缺失时生成，避免覆盖用户手动维护的 MCP 配置。
     */
    public Path generateIfAbsent(Path workDir, String mcpServerUrl) throws IOException {
        Path mcpFile = workDir.resolve("mcp-config.json");
        if (Files.exists(mcpFile)) {
            return mcpFile;
        }
        return generate(workDir, mcpServerUrl);
    }

    private Map<String, Object> buildMcpConfig(String mcpServerUrl) {
        Map<String, Object> server = new HashMap<>();
        server.put("type", "sse");
        server.put("url", mcpServerUrl);

        Map<String, Object> mcpServers = new HashMap<>();
        mcpServers.put("agents-capability", server);

        Map<String, Object> config = new HashMap<>();
        config.put("mcpServers", mcpServers);
        return config;
    }
}
