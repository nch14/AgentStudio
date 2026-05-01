package com.chenhaonee.agents.app.infrastructure.tool;

import com.chenhaonee.agents.connect.spi.tool.ToolDescriptor;
import com.chenhaonee.agents.connect.spi.tool.ToolRegistry;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 当前阶段的默认工具注册表实现。
 */
@Component
public class EmptyToolRegistry implements ToolRegistry {

    @Override
    public List<ToolDescriptor> listAll() {
        return List.of();
    }

    @Override
    public Optional<ToolDescriptor> findByCode(String toolCode) {
        return Optional.empty();
    }

    @Override
    public List<ToolDescriptor> listAuthorizedTools(String agentCode) {
        return List.of();
    }
}
