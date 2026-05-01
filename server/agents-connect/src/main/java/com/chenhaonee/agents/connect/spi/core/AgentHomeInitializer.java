package com.chenhaonee.agents.connect.spi.core;

import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import java.io.IOException;
import java.nio.file.Path;

/**
 * Agent Home 初始化 SPI。
 * 由框架在 Agent 创建时调用，负责在已创建的 agentHome 目录下写入实现专属的初始化文件。
 * 框架保证调用时 agentHome 目录已存在。
 */
public interface AgentHomeInitializer {

    /**
     * 声明此初始化器支持的 Agent 实现类型。
     */
    AgentProvider supportedType();

    /**
     * 写入 agent home 的实现专属初始化文件（幂等，仅写不存在的文件）。
     *
     * @param agentCode agent 编码
     * @param agentHome agent home 目录的物理路径（框架已创建）
     */
    void initHome(String agentCode, Path agentHome) throws IOException;
}
