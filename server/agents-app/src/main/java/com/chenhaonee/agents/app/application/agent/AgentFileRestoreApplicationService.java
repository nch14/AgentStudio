package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.common.oss.OssObjectService;
import com.chenhaonee.agents.domain.agent.model.CloudFile;
import com.chenhaonee.agents.domain.agent.repository.CloudFileRepository;
import com.chenhaonee.agents.domain.agent.service.LocalFileDomainService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 从 OSS 备份恢复文件到本地。
 */
@Service
public class AgentFileRestoreApplicationService {

    private static final Logger log = LoggerFactory.getLogger(AgentFileRestoreApplicationService.class);

    private final CloudFileRepository cloudFileRepository;
    private final OssObjectService ossObjectService;
    private final LocalFileDomainService localFileDomainService;

    public AgentFileRestoreApplicationService(CloudFileRepository cloudFileRepository,
                                              OssObjectService ossObjectService,
                                              LocalFileDomainService localFileDomainService) {
        this.cloudFileRepository = cloudFileRepository;
        this.ossObjectService = ossObjectService;
        this.localFileDomainService = localFileDomainService;
    }

    /**
     * 从 OSS 恢复指定 agent 的所有已备份文件到本地。
     */
    @Transactional(rollbackFor = Exception.class)
    public int restoreFromBackup(String agentCode) {
        List<CloudFile> files = cloudFileRepository.findByAgentCodeAndValidIsTrue(agentCode);
        int restored = 0;

        for (CloudFile file : files) {
            if (file.getOssKey() == null) {
                continue;
            }
            try {
                Path agentHome = localFileDomainService.getAgentHomePath(agentCode);
                String relativeDirPath = file.getPath().startsWith("home/")
                        ? file.getPath().substring("home/".length())
                        : file.getPath();
                Path resolved = agentHome.resolve(relativeDirPath).resolve(file.getName()).normalize();
                if (!resolved.startsWith(agentHome.normalize())) {
                    log.warn("restoreFromBackup: path traversal detected, skipping {}/{}", file.getPath(), file.getName());
                    continue;
                }

                String content = ossObjectService.readAsString(file.getOssKey());
                Files.createDirectories(resolved.getParent());
                Files.writeString(resolved, content);

                // 关键：恢复 mtime 与 lastSyncTime 一致，下次备份不会重复上传
                if (file.getLastSyncTime() != null) {
                    Files.setLastModifiedTime(resolved,
                            java.nio.file.attribute.FileTime.fromMillis(file.getLastSyncTime().getTime()));
                }

                restored++;
            } catch (IOException e) {
                log.warn("Failed to restore file {}/{} for agent {}: {}",
                        file.getPath(), file.getName(), agentCode, e.getMessage());
            }
        }

        log.info("Restore from backup completed for agent={}: restored={}", agentCode, restored);
        return restored;
    }
}
