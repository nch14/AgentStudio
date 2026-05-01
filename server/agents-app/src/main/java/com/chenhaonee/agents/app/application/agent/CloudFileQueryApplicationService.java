package com.chenhaonee.agents.app.application.agent;

import com.chenhaonee.agents.common.oss.OssObjectService;
import com.chenhaonee.agents.domain.agent.model.CloudFile;
import com.chenhaonee.agents.domain.agent.repository.CloudFileRepository;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.stereotype.Service;

/**
 * 云端文件查询服务（基于 CloudFile DB 记录 + OSS）。
 */
@Service
public class CloudFileQueryApplicationService {

    private final CloudFileRepository cloudFileRepository;
    private final OssObjectService ossObjectService;

    public CloudFileQueryApplicationService(CloudFileRepository cloudFileRepository,
                                            OssObjectService ossObjectService) {
        this.cloudFileRepository = cloudFileRepository;
        this.ossObjectService = ossObjectService;
    }

    /**
     * 列举指定 agent 的所有已备份文件（ossKey 不为空）。
     */
    public List<CloudFile> listCloudFiles(String agentCode) {
        return cloudFileRepository.findByAgentCodeAndValidIsTrue(agentCode).stream()
                .filter(f -> f.getOssKey() != null)
                .toList();
    }

    /**
     * 按 fileCode 获取备份文件记录。
     */
    public CloudFile getCloudFile(String agentCode, String fileCode) {
        CloudFile file = cloudFileRepository.findByCode(fileCode)
                .orElseThrow(() -> new NoSuchElementException("cloud file not found: " + fileCode));
        if (!agentCode.equals(file.getAgentCode())) {
            throw new IllegalArgumentException("file does not belong to agent: " + agentCode);
        }
        if (file.getOssKey() == null) {
            throw new IllegalStateException("file has not been backed up to cloud: " + fileCode);
        }
        return file;
    }

    /**
     * 从 OSS 读取备份文件内容。
     */
    public String getCloudFileContent(String fileCode) {
        CloudFile file = cloudFileRepository.findByCode(fileCode)
                .orElseThrow(() -> new NoSuchElementException("cloud file not found: " + fileCode));
        if (file.getOssKey() == null) {
            throw new IllegalStateException("file has not been backed up to cloud: " + fileCode);
        }
        return ossObjectService.readAsString(file.getOssKey());
    }

    public CloudFile getFileByCode(String fileCode) {
        return cloudFileRepository.findByCode(fileCode)
                .orElseThrow(() -> new NoSuchElementException("cloud file not found: " + fileCode));
    }

    /**
     * 按 fileCode 获取文件，并校验其归属于指定的 agentCode。
     */
    public CloudFile getFileByAgentAndCode(String agentCode, String fileCode) {
        CloudFile file = getFileByCode(fileCode);
        if (!agentCode.equals(file.getAgentCode())) {
            throw new IllegalArgumentException("file does not belong to agent: " + agentCode);
        }
        return file;
    }
}
