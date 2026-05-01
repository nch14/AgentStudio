package com.chenhaonee.agents.claudecode.stream;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 逐行解析 Claude Code stream-json 输出。
 */
public class StreamJsonParser {

    private static final Logger log = LoggerFactory.getLogger(StreamJsonParser.class);

    /**
     * 解析一行 JSON 字符串为 StreamJsonEvent。
     * 非 JSON 行或空行记 WARN 并返回 null。
     */
    public StreamJsonEvent parseLine(String line) {
        if (line == null || line.isBlank()) {
            return null;
        }
        try {
            return JSON.parseObject(line, StreamJsonEvent.class);
        } catch (JSONException e) {
            log.warn("Failed to parse stream-json line, skipping: {}", line);
            return null;
        }
    }
}
