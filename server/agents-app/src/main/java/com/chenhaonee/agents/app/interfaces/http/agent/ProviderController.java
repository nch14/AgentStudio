package com.chenhaonee.agents.app.interfaces.http.agent;

import com.chenhaonee.agents.app.application.agent.ProviderCapabilityService;
import com.chenhaonee.agents.app.application.agent.ProviderCapabilityService.ProviderCapabilities;
import com.chenhaonee.agents.app.application.agent.ProviderConfigValidationService;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.ConfigKeyDescriptorDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.ProviderCapabilityDTO;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.connect.spi.core.AgentProviderConfigDescriptor.ConfigKeyDescriptor;
import com.chenhaonee.agents.domain.agent.model.AgentProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provider 类型与配置元信息查询接口。
 */
@Tag(name = "Provider", description = "Provider 支持类型与配置元信息")
@RestController
@RequestMapping("/api/v1/providers")
@RequiredArgsConstructor
public class ProviderController {

    private final ProviderCapabilityService providerCapabilityService;
    private final ProviderConfigValidationService providerConfigValidationService;

    @Operation(summary = "查询各 Provider 支持的能力")
    @GetMapping("/supports")
    public Response<List<ProviderCapabilityDTO>> supports() {
        try {
            List<ProviderCapabilityDTO> result = new ArrayList<>();
            for (var entry : providerCapabilityService.getAllCapabilities().entrySet()) {
                AgentProvider provider = entry.getKey();
                ProviderCapabilities caps = entry.getValue();
                result.add(new ProviderCapabilityDTO(
                        provider.name(), caps.supportChat(), caps.supportMessages(), caps.supportTask()));
            }
            return Response.success(result);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查询 Agent 配置描述符")
    @GetMapping("/configs")
    public Response<List<ConfigKeyDescriptorDTO>> configDescriptors() {
        try {
            var allDescriptors = providerConfigValidationService.getAllDescriptors();
            List<ConfigKeyDescriptorDTO> result = new ArrayList<>();
            for (var entry : allDescriptors.entrySet()) {
                String providerName = entry.getKey().name();
                result.addAll(entry.getValue().stream()
                        .map(desc -> toDescriptorDTO(providerName, desc))
                        .toList());
            }
            return Response.success(result);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    private ConfigKeyDescriptorDTO toDescriptorDTO(String provider, ConfigKeyDescriptor desc) {
        return new ConfigKeyDescriptorDTO(
                provider, desc.key(), desc.displayName(), desc.description(), desc.required(), desc.defaultValue());
    }
}
