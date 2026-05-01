package com.chenhaonee.agents.common.utils;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Locale;
import java.util.Set;

/**
 * OSS 上传相关工具。
 */
public final class OssUploadUtils {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif", "svg", "avif");
    private static final Set<String> DOC_EXTENSIONS = Set.of("doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "md");

    private OssUploadUtils() {
    }

    /**
     * 清理上传文件名，避免目录穿越和非法字符。
     */
    public static String sanitizeFileName(String filename) {
        String raw = StringUtils.defaultIfBlank(filename, "file");
        String name = FilenameUtils.getName(raw).trim();
        if (name.isEmpty()) {
            name = "file";
        }
        String sanitized = name
                .replace("..", "_")
                .replaceAll("[\\\\/:*?\"<>|\\s]+", "-")
                .replaceAll("-+", "-");
        sanitized = StringUtils.strip(sanitized, "-.");
        return StringUtils.defaultIfBlank(sanitized, "file");
    }

    /**
     * 清理对象子目录，保留层级结构并去除危险片段。
     */
    public static String sanitizeDir(String dir) {
        if (StringUtils.isBlank(dir)) {
            return "";
        }
        String normalized = dir.trim().replace("\\", "/");
        normalized = normalized.replaceAll("^/+", "").replaceAll("/+$", "");
        normalized = normalized.replaceAll("/+", "/");
        normalized = normalized.replace("..", "_");
        normalized = normalized.replaceAll("[^a-zA-Z0-9._/\\-\\u4e00-\\u9fa5]", "_");
        return normalized;
    }

    /**
     * 根据 contentType 与扩展名推断对象一级目录。
     */
    public static String chooseTypePrefix(String contentType, String filename) {
        String extension = extension(filename);
        String ct = StringUtils.defaultString(contentType).toLowerCase(Locale.ROOT);
        if (ct.startsWith("image/") || IMAGE_EXTENSIONS.contains(extension)) {
            return "images";
        }
        if ("application/pdf".equals(ct) || "pdf".equals(extension)) {
            return "pdfs";
        }
        if (ct.startsWith("video/")) {
            return "videos";
        }
        if (ct.startsWith("audio/")) {
            return "audios";
        }
        if (DOC_EXTENSIONS.contains(extension)) {
            return "docs";
        }
        return "files";
    }

    /**
     * 根据扩展名推断 Content-Type。
     */
    public static String guessContentType(String filename) {
        return switch (extension(filename)) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "svg" -> "image/svg+xml";
            case "avif" -> "image/avif";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain; charset=utf-8";
            case "md" -> "text/markdown; charset=utf-8";
            case "mp4" -> "video/mp4";
            case "m3u8" -> "application/x-mpegURL";
            case "ts" -> "video/mp2t";
            case "json" -> "application/json";
            default -> "application/octet-stream";
        };
    }

    private static String extension(String filename) {
        return FilenameUtils.getExtension(StringUtils.defaultString(filename)).toLowerCase(Locale.ROOT);
    }
}
