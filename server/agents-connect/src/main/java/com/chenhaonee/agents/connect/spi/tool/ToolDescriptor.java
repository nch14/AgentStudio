package com.chenhaonee.agents.connect.spi.tool;

import java.util.Map;

/**
 * 工具描述值对象。
 * <p>
 * 描述平台上注册的一个可用工具的元数据。
 */
public record ToolDescriptor(
        String toolCode,
        String displayName,
        String description,
        String group,
        Map<String, Object> parameterSchema
) {
}
