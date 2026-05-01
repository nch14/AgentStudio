package com.chenhaonee.agents.domain.agent.service;

import com.chenhaonee.agents.common.config.AgentWorkspaceProperties;
import com.chenhaonee.agents.common.validator.ValidationUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.stereotype.Service;

/**
 * Agent 本地文件系统操作服务。无任何 DB 依赖，直接读写磁盘。
 */
@Service
public class LocalFileDomainService {

    private static final String HOME_PREFIX = "home/";
    private static final String WORKSPACE_PREFIX = "workspace/";

    private final AgentWorkspaceProperties workspaceProperties;

    public LocalFileDomainService(AgentWorkspaceProperties workspaceProperties) {
        this.workspaceProperties = workspaceProperties;
    }

    public String readFileByPath(String agentCode, String logicalPath) {
        Path localPath = resolveLocalPath(agentCode, parseLogicalPath(logicalPath));
        try {
            return Files.readString(localPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to read agent file: " + logicalPath, e);
        }
    }

    public void writeFileByPath(String agentCode, String logicalPath, String content) {
        Path localPath = resolveLocalPath(agentCode, parseLogicalPath(logicalPath));
        try {
            Files.createDirectories(localPath.getParent());
            Files.writeString(localPath, content == null ? "" : content);
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to write agent file: " + logicalPath, e);
        }
    }

    public void appendFileByPath(String agentCode, String logicalPath, String content) {
        Path localPath = resolveLocalPath(agentCode, parseLogicalPath(logicalPath));
        try {
            Files.createDirectories(localPath.getParent());
            String existing = Files.exists(localPath) ? Files.readString(localPath) : "";
            Files.writeString(localPath, existing + (content == null ? "" : content));
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to append agent file: " + logicalPath, e);
        }
    }

    public void patchFileByPath(String agentCode, String logicalPath, String targetText, String replacementText) {
        Path localPath = resolveLocalPath(agentCode, parseLogicalPath(logicalPath));
        try {
            String existingContent = Files.readString(localPath);
            if (existingContent.isEmpty()) {
                throw new IllegalArgumentException("agent file not found: " + logicalPath);
            }
            String normalizedTarget = ValidationUtils.requireText(targetText, "targetText");
            int startIndex = existingContent.indexOf(normalizedTarget);
            if (startIndex < 0) {
                throw new IllegalArgumentException("target text not found in file: " + logicalPath);
            }
            String replacement = replacementText == null ? "" : replacementText;
            String patchedContent = existingContent.substring(0, startIndex)
                    + replacement
                    + existingContent.substring(startIndex + normalizedTarget.length());
            Files.writeString(localPath, patchedContent);
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to patch agent file: " + logicalPath, e);
        }
    }

    public void deleteFileByPath(String agentCode, String logicalPath) {
        Path localPath = resolveLocalPath(agentCode, parseLogicalPath(logicalPath));
        try {
            Files.deleteIfExists(localPath);
        } catch (IOException e) {
            throw new IllegalArgumentException("failed to delete agent file: " + logicalPath, e);
        }
    }

    /**
     * 返回指定 agent 的本地文件系统根目录。
     */
    public Path getAgentHomePath(String agentCode) {
        return Paths.get(workspaceProperties.getWorkspacePath(), agentCode);
    }

    // ─── private helpers ──────────────────────────────────────────────────────

    private ParsedLogicalPath parseLogicalPath(String logicalPath) {
        String normalizedPath = ValidationUtils.requireText(logicalPath, "path").replace('\\', '/');
        validateRootPrefix(normalizedPath);
        if (normalizedPath.contains("../") || normalizedPath.contains("..\\")) {
            throw new IllegalArgumentException("path traversal is not allowed: " + logicalPath);
        }
        int splitIndex = normalizedPath.lastIndexOf('/');
        if (splitIndex < 0) {
            throw new IllegalArgumentException("path must include root directory: " + logicalPath);
        }
        String path = normalizedPath.substring(0, splitIndex + 1);
        String name = ValidationUtils.requireText(normalizedPath.substring(splitIndex + 1), "path.name");
        return new ParsedLogicalPath(path, name);
    }

    private void validateRootPrefix(String logicalPath) {
        if (!(logicalPath.startsWith(HOME_PREFIX) || logicalPath.startsWith(WORKSPACE_PREFIX))) {
            throw new IllegalArgumentException("path must start with home/ or workspace/: " + logicalPath);
        }
    }

    private Path resolveLocalPath(String agentCode, ParsedLogicalPath parsed) {
        Path agentHome = getAgentHomePath(agentCode).normalize();
        String relativeDirPath = parsed.path.startsWith(HOME_PREFIX)
                ? parsed.path.substring(HOME_PREFIX.length())
                : parsed.path;
        Path resolved = agentHome.resolve(relativeDirPath).resolve(parsed.name).normalize();
        if (!resolved.startsWith(agentHome)) {
            throw new IllegalArgumentException("path traversal detected: " + parsed.path + parsed.name);
        }
        return resolved;
    }

    private record ParsedLogicalPath(String path, String name) {
    }
}
