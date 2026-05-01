package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.connect.spi.core.AgentProviderConfigDescriptor;
import com.chenhaonee.agents.connect.spi.core.AgentProviderConfigDescriptor.ConfigKeyDescriptor;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 根据 AgentProviderConfigDescriptor 对 providerConfig 进行校验。
 */
@Service
public class ProviderConfigValidationService {

    private final Map<AgentProvider, AgentProviderConfigDescriptor> descriptorMap;

    public ProviderConfigValidationService(List<AgentProviderConfigDescriptor> descriptors) {
        this.descriptorMap = descriptors.stream()
                .collect(Collectors.toMap(AgentProviderConfigDescriptor::supportedType, Function.identity()));
    }

    /**
     * 校验 providerConfig 是否符合对应 provider 的声明。
     */
    public void validate(AgentProvider provider, Map<String, String> providerConfig) {
        AgentProviderConfigDescriptor descriptor = descriptorMap.get(provider);
        if (descriptor == null) {
            throw new IllegalArgumentException("unsupported agent provider: " + provider);
        }
        if (providerConfig == null || providerConfig.isEmpty()) {
            // 检查是否有 required 的配置项
            for (ConfigKeyDescriptor keyDesc : descriptor.supportedConfigKeys()) {
                if (keyDesc.required()) {
                    throw new IllegalArgumentException("missing required config key: " + keyDesc.key());
                }
            }
            return;
        }

        List<String> allowedKeys = descriptor.supportedConfigKeys().stream()
                .map(ConfigKeyDescriptor::key)
                .toList();

        // 检查是否有未知的 key
        for (String key : providerConfig.keySet()) {
            if (!allowedKeys.contains(key)) {
                throw new IllegalArgumentException("unknown config key for provider " + provider + ": " + key);
            }
        }

        // 检查 required 的 key 是否存在
        for (ConfigKeyDescriptor keyDesc : descriptor.supportedConfigKeys()) {
            if (keyDesc.required() && !providerConfig.containsKey(keyDesc.key())) {
                throw new IllegalArgumentException("missing required config key: " + keyDesc.key());
            }
        }
    }

    /**
     * 获取指定 provider 的配置描述符列表。
     */
    public List<ConfigKeyDescriptor> getConfigDescriptor(AgentProvider provider) {
        AgentProviderConfigDescriptor descriptor = descriptorMap.get(provider);
        if (descriptor == null) {
            throw new IllegalArgumentException("unsupported agent provider: " + provider);
        }
        return descriptor.supportedConfigKeys();
    }

    /**
     * 获取所有 provider 的配置描述。
     */
    public Map<AgentProvider, List<ConfigKeyDescriptor>> getAllDescriptors() {
        return descriptorMap.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().supportedConfigKeys()));
    }
}
