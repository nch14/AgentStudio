package com.chenhaonee.agents.app.interfaces.http.agent;

import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentFileEntryDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentFileEntryType;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.CloudFileDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.LocalFileDetailDTO;
import com.chenhaonee.agents.app.application.agent.LocalFileApplicationService.LocalFileEntry;
import com.chenhaonee.agents.domain.agent.model.Agent;
import com.chenhaonee.agents.domain.agent.model.CloudFile;
import org.springframework.stereotype.Component;

/**
 * Agent HTTP DTO 装配器。
 */
@Component
public class AgentHttpAssembler {

    public AgentDetailDTO toDetailResponse(Agent agent) {
        return new AgentDetailDTO(
                agent.getCode(),
                agent.getName(),
                agent.getResponsibility(),
                agent.getProvider() != null ? agent.getProvider().name() : null,
                agent.getStatus() != null ? agent.getStatus().name() : null,
                agent.getProviderConfig(),
                agent.getCreateTime() != null ? agent.getCreateTime().toString() : null,
                agent.getUpdateTime() != null ? agent.getUpdateTime().toString() : null);
    }

    public LocalFileDetailDTO toLocalFileDetailResponse(LocalFileEntry entry) {
        return new LocalFileDetailDTO(null, entry.path(), entry.name(), entry.size(), entry.lastModifiedTime());
    }

    public AgentFileEntryDTO toLocalFileEntryResponse(LocalFileEntry entry) {
        return new AgentFileEntryDTO(
                AgentFileEntryType.FILE,
                entry.name(),
                entry.path(),
                null,
                entry.size(),
                entry.lastModifiedTime());
    }

    public CloudFileDetailDTO toCloudFileDetailResponse(CloudFile file) {
        return new CloudFileDetailDTO(
                file.getCode(),
                file.getAgentCode(),
                file.getPath(),
                file.getName(),
                file.getFileSize(),
                file.getOssKey(),
                file.getLastSyncTime() != null ? file.getLastSyncTime().toString() : null,
                file.getCreateTime() != null ? file.getCreateTime().toString() : null,
                file.getUpdateTime() != null ? file.getUpdateTime().toString() : null);
    }
}
