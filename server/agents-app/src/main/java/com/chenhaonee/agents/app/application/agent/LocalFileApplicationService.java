package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.domain.agent.service.LocalFileDomainService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;

/**
 * 本地文件应用服务。无任何 DB 依赖，直接操作文件系统。
 */
@Service
public class LocalFileApplicationService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final LocalFileDomainService localFileDomainService;

    public LocalFileApplicationService(LocalFileDomainService localFileDomainService) {
        this.localFileDomainService = localFileDomainService;
    }

    /**
     * 扫描 agent home 下所有文件（跳过隐藏目录和框架文件）。
     *
     * @param path    限定路径，不为空时只返回该路径下的文件
     * @param exclude 排除路径，该路径下的文件不扫描
     */
    public List<LocalFileEntry> listFiles(String agentCode, String path, String exclude) {
        Path agentHome = localFileDomainService.getAgentHomePath(agentCode);
        return scanFiles(agentCode, agentHome, path, exclude);
    }

    /**
     * 扫描指定目录下的文件（不递归子目录）。
     */
    public List<LocalFileEntry> listFilesByPath(String agentCode, String logicalPath) {
        Path agentHome = localFileDomainService.getAgentHomePath(agentCode);
        String normalizedPath = normalizeDir(logicalPath);
        Path targetDir = agentHome.resolve(toLocalRelative(normalizedPath));
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            return List.of();
        }
        List<LocalFileEntry> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(targetDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> !isFrameworkFile(p.getFileName().toString()))
                    .forEach(p -> result.add(toEntry(agentHome, p)));
        } catch (IOException e) {
            throw new RuntimeException("failed to list files in path: " + logicalPath, e);
        }
        return result;
    }

    /**
     * 列举指定目录下的子目录名（相对逻辑路径）。
     */
    public List<String> listSubPaths(String agentCode, String logicalPath) {
        Path agentHome = localFileDomainService.getAgentHomePath(agentCode);
        String normalizedPath = normalizeDir(logicalPath);
        Path targetDir = agentHome.resolve(toLocalRelative(normalizedPath));
        if (!Files.exists(targetDir) || !Files.isDirectory(targetDir)) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(targetDir)) {
            stream.filter(Files::isDirectory)
                    .filter(p -> !p.getFileName().toString().startsWith("."))
                    .forEach(p -> result.add(normalizedPath + p.getFileName() + "/"));
        } catch (IOException e) {
            throw new RuntimeException("failed to list subpaths: " + logicalPath, e);
        }
        return result;
    }

    /**
     * 创建或覆盖写入文件。
     */
    public void createFile(String agentCode, String logicalPath, String content) {
        localFileDomainService.writeFileByPath(agentCode, logicalPath, content);
    }

    /**
     * 读取文件内容（供 Controller 使用）。
     */
    public String readFileByPathForContent(String agentCode, String logicalPath) {
        return localFileDomainService.readFileByPath(agentCode, logicalPath);
    }

    /**
     * 覆盖写入文件内容。
     */
    public void writeFile(String agentCode, String logicalPath, String content) {
        localFileDomainService.writeFileByPath(agentCode, logicalPath, content);
    }

    /**
     * 删除文件。
     */
    public void deleteFile(String agentCode, String logicalPath) {
        localFileDomainService.deleteFileByPath(agentCode, logicalPath);
    }

    // ─── helpers ──────────────────────────────────────────────────────────────

    private List<LocalFileEntry> scanFiles(String agentCode, Path agentHome, String include, String exclude) {
        if (!Files.exists(agentHome) || !Files.isDirectory(agentHome)) {
            return List.of();
        }
        String normalizedInclude = include == null || include.isBlank() ? "home/" : normalizeDir(include);
        String normalizedExclude = exclude != null && !exclude.isBlank() ? normalizeDir(exclude) : null;
        List<LocalFileEntry> result = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(agentHome)) {
            stream.filter(Files::isRegularFile).forEach(p -> {
                String relative = agentHome.relativize(p).toString().replace('\\', '/');
                String logicalDir = "home/" + (relative.contains("/")
                        ? relative.substring(0, relative.lastIndexOf('/') + 1)
                        : "");
                for (String part : relative.split("/")) {
                    if (part.startsWith(".")) {
                        return;
                    }
                }
                if (isFrameworkFile(p.getFileName().toString())) {
                    return;
                }
                if (!logicalDir.startsWith(normalizedInclude)) {
                    return;
                }
                if (normalizedExclude != null && logicalDir.startsWith(normalizedExclude)) {
                    return;
                }
                Long size = null;
                String lastModified = null;
                try {
                    size = Files.size(p);
                    FileTime ft = Files.getLastModifiedTime(p);
                    lastModified = ft.toInstant().atOffset(java.time.ZoneOffset.UTC).format(FORMATTER);
                } catch (IOException ignored) {
                }
                result.add(new LocalFileEntry(logicalDir, p.getFileName().toString(), size, lastModified));
            });
        } catch (IOException e) {
            throw new RuntimeException("failed to scan local files for agent: " + agentCode, e);
        }
        return result;
    }

    private LocalFileEntry toEntry(Path agentHome, Path file) {
        String relative = agentHome.relativize(file).toString().replace('\\', '/');
        String[] parts = relative.split("/");
        String parentDir = parts.length > 1
                ? String.join("/", java.util.Arrays.copyOf(parts, parts.length - 1)) + "/"
                : "";
        Long size = null;
        String lastModified = null;
        try {
            size = Files.size(file);
            FileTime ft = Files.getLastModifiedTime(file);
            lastModified = ft.toInstant().atOffset(java.time.ZoneOffset.UTC).format(FORMATTER);
        } catch (IOException ignored) {
        }
        return new LocalFileEntry("home/" + parentDir, file.getFileName().toString(), size, lastModified);
    }

    private String normalizeDir(String path) {
        if (path == null || path.isBlank()) {
            return "";
        }
        String normalized = path.replace('\\', '/');
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /** home/xxx/ → xxx/ (去掉 home/ 前缀供 Path.resolve 使用) */
    private String toLocalRelative(String logicalDir) {
        if (logicalDir.startsWith("home/")) {
            return logicalDir.substring("home/".length());
        }
        return logicalDir;
    }

    private boolean isFrameworkFile(String name) {
        // 当前不过滤任何文件，保留方法供未来扩展
        return false;
    }

    /**
     * 本地文件条目（纯文件系统数据，无 DB code）。
     */
    public record LocalFileEntry(String path, String name, Long size, String lastModifiedTime) {
    }
}
