package com.chenhaonee.agents.app.application.agent.backup;

import com.chenhaonee.agents.common.config.AgentWorkspaceProperties;
import com.chenhaonee.agents.common.oss.OssObjectService;
import com.chenhaonee.agents.domain.agent.model.CloudFile;
import com.chenhaonee.agents.domain.agent.repository.CloudFileRepository;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 备份核心逻辑：扫描本地文件，基于 mtime 比对增量上传到 OSS。
 */
@Service
public class BackupService {

    private static final Set<String> SKIP_NAMES = Set.of("CLAUDE.md", "mcp-config.json", "task.md", ".DS_Store");

    private final CloudFileRepository cloudFileRepository;
    private final OssObjectService ossObjectService;
    private final AgentWorkspaceProperties workspaceProperties;

    public BackupService(CloudFileRepository cloudFileRepository,
                         OssObjectService ossObjectService,
                         AgentWorkspaceProperties workspaceProperties) {
        this.cloudFileRepository = cloudFileRepository;
        this.ossObjectService = ossObjectService;
        this.workspaceProperties = workspaceProperties;
    }

    /**
     * 对所有 agent 执行备份。
     */
    public BackupStats backupAll() {
        Path workspaceRoot = Paths.get(workspaceProperties.getWorkspacePath());
        if (!Files.exists(workspaceRoot) || !Files.isDirectory(workspaceRoot)) {
            return new BackupStats(0, 0, 0);
        }

        int totalBackedUp = 0;
        int totalSkipped = 0;
        int totalFailed = 0;

        try (Stream<Path> dirs = Files.list(workspaceRoot)) {
            for (Path agentDir : dirs.filter(Files::isDirectory).toList()) {
                String agentCode = agentDir.getFileName().toString();
                BackupStats stats = backupAgent(agentCode);
                totalBackedUp += stats.backedUpCount;
                totalSkipped += stats.skippedCount;
                totalFailed += stats.failedCount;
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(BackupService.class).error("Failed to scan workspace root: {}", e.getMessage());
        }

        return new BackupStats(totalBackedUp, totalSkipped, totalFailed);
    }

    /**
     * 对单个 agent 执行备份。
     */
    public BackupStats backupAgent(String agentCode) {
        Path agentHome = Paths.get(workspaceProperties.getWorkspacePath(), agentCode);
        if (!Files.exists(agentHome) || !Files.isDirectory(agentHome)) {
            return new BackupStats(0, 0, 0);
        }

        int backedUp = 0;
        int skipped = 0;
        int failed = 0;

        try (Stream<Path> stream = Files.walk(agentHome)) {
            for (Path file : stream.filter(Files::isRegularFile).toList()) {
                try {
                    if (shouldSkip(file)) {
                        skipped++;
                        continue;
                    }

                    String relativePath = agentHome.relativize(file).toString().replace('\\', '/');
                    String logicalPath = "home/" + relativePath;
                    ParsedPath parsed = parseLogicalPath(logicalPath);

                    // 查 DB 看是否已有备份记录
                    Optional<CloudFile> existing = cloudFileRepository
                            .findByAgentCodeAndPathAndNameAndValidIsTrue(agentCode, parsed.path(), parsed.name());

                    if (existing.isPresent()) {
                        CloudFile record = existing.get();
                        FileTime currentMtime = Files.getLastModifiedTime(file);
                        // lastSyncTime 存的是文件 mtime，相等说明未变更
                        if (record.getLastSyncTime() != null
                                && record.getLastSyncTime().getTime() == currentMtime.toMillis()) {
                            skipped++;
                            continue;
                        }
                    }

                    // 需要备份
                    String content = Files.readString(file);
                    byte[] bytes = content.getBytes();
                    String ossKey = "agent-files/" + agentCode + "/" + relativePath;
                    FileTime mtime = Files.getLastModifiedTime(file);

                    ossObjectService.uploadBytes(bytes, ossKey, guessContentType(file.getFileName().toString()));

                    // 更新或创建 CloudFile 记录
                    CloudFile record = existing.orElseGet(() -> {
                        CloudFile f = new CloudFile();
                        f.initialize(agentCode, parsed.path(), parsed.name());
                        return f;
                    });
                    record.setOssKey(ossKey);
                    record.setFileSize((long) bytes.length);
                    record.setLastSyncTime(new Date(mtime.toMillis()));
                    cloudFileRepository.save(record);

                    backedUp++;
                } catch (Exception e) {
                    failed++;
                    LoggerFactory.getLogger(BackupService.class)
                            .warn("Backup failed for agent={} file={}: {}", agentCode, file, e.getMessage());
                }
            }
        } catch (IOException e) {
            LoggerFactory.getLogger(BackupService.class)
                    .error("Failed to walk agent home for backup: agent={}", agentCode, e);
        }

        return new BackupStats(backedUp, skipped, failed);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private boolean shouldSkip(Path file) {
        String fileName = file.getFileName().toString();
        if (SKIP_NAMES.contains(fileName) || fileName.endsWith(".log")) {
            return true;
        }
        // 隐藏目录中的文件
        Path relative = file;
        while (relative.getParent() != null) {
            if (relative.getFileName().toString().startsWith(".")) {
                return true;
            }
            relative = relative.getParent();
        }
        return false;
    }

    private ParsedPath parseLogicalPath(String logicalPath) {
        int lastSlash = logicalPath.lastIndexOf('/');
        String path = logicalPath.substring(0, lastSlash + 1);
        String name = logicalPath.substring(lastSlash + 1);
        return new ParsedPath(path, name);
    }

    private String guessContentType(String name) {
        if (name.endsWith(".md")) return "text/markdown";
        if (name.endsWith(".txt")) return "text/plain";
        if (name.endsWith(".json")) return "application/json";
        if (name.endsWith(".yaml") || name.endsWith(".yml")) return "text/yaml";
        return "application/octet-stream";
    }

    private record ParsedPath(String path, String name) {
    }

    public record BackupStats(int backedUpCount, int skippedCount, int failedCount) {
    }
}
