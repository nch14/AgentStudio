package com.chenhaonee.agents.connect.spi.tool;

import java.util.List;
import java.util.Optional;

/**
 * 平台级工具注册表 SPI。
 * <p>
 * 管理系统中所有已安装的工具，
 * 并提供按 Agent 授权过滤的查询能力。
 */
public interface ToolRegistry {

    List<ToolDescriptor> listAll();

    Optional<ToolDescriptor> findByCode(String toolCode);

    /**
     * 获取指定 Agent 被授权使用的工具列表。
     */
    List<ToolDescriptor> listAuthorizedTools(String agentCode);
}
