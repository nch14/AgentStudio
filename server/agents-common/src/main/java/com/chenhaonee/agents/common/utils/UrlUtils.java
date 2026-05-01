package com.chenhaonee.agents.common.utils;

import org.apache.commons.lang3.StringUtils;

/**
 * URL 拼接工具。
 */
public final class UrlUtils {

    private UrlUtils() {
    }

    /**
     * 规范化 HTTP 根地址。
     */
    public static String normalizeHttpRoot(String rawRoot) {
        String normalized = StringUtils.trimToEmpty(rawRoot);
        if (normalized.isEmpty()) {
            return normalized;
        }
        while (normalized.endsWith("/") && normalized.length() > 1) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        if (normalized.startsWith("/")) {
            return normalized;
        }
        if (normalized.startsWith("//")) {
            return normalized;
        }
        if (normalized.contains("://")) {
            return normalized;
        }
        return "http://" + normalized;
    }

    /**
     * 拼接 HTTP 根地址与路径。
     */
    public static String joinHttpRootAndPath(String httpRoot, String path) {
        String normalizedRoot = normalizeHttpRoot(httpRoot);
        if (StringUtils.isBlank(path)) {
            return normalizedRoot;
        }
        if (path.startsWith("http://") || path.startsWith("https://") || path.startsWith("//")) {
            return path;
        }

        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        if (StringUtils.isBlank(normalizedRoot) || "/".equals(normalizedRoot)) {
            return normalizedPath;
        }
        if (normalizedRoot.endsWith("/")) {
            return normalizedRoot + normalizedPath.substring(1);
        }
        return normalizedRoot + normalizedPath;
    }
}
