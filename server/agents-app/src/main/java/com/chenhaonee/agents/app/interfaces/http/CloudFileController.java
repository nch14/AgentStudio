package com.chenhaonee.agents.app.interfaces.http;

import com.chenhaonee.agents.app.application.agent.AgentFileRestoreApplicationService;
import com.chenhaonee.agents.app.application.agent.CloudFileQueryApplicationService;
import com.chenhaonee.agents.app.application.agent.backup.BackupService;
import com.chenhaonee.agents.app.interfaces.http.agent.AgentHttpAssembler;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.CloudFileDetailDTO;
import com.chenhaonee.agents.domain.agent.model.CloudFile;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 云端文件查看与恢复 API — 基于 CloudFile DB 记录（ossKey 不为空）。
 */
@Tag(name = "CloudFile", description = "Agent 云端文件")
@RestController
@RequestMapping("/api/v1/agents/{agentCode}/cloud-files")
public class CloudFileController {

    private final CloudFileQueryApplicationService cloudFileQueryService;
    private final AgentFileRestoreApplicationService restoreService;
    private final BackupService backupService;
    private final AgentHttpAssembler agentHttpAssembler;

    public CloudFileController(CloudFileQueryApplicationService cloudFileQueryService,
                               AgentFileRestoreApplicationService restoreService,
                               BackupService backupService,
                               AgentHttpAssembler agentHttpAssembler) {
        this.cloudFileQueryService = cloudFileQueryService;
        this.restoreService = restoreService;
        this.backupService = backupService;
        this.agentHttpAssembler = agentHttpAssembler;
    }

    @Operation(summary = "列举所有已备份的云端文件")
    @GetMapping
    public Response<List<CloudFileDetailDTO>> listCloudFiles(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            List<CloudFileDetailDTO> files = cloudFileQueryService.listCloudFiles(agentCode)
                    .stream()
                    .map(agentHttpAssembler::toCloudFileDetailResponse)
                    .toList();
            return Response.success(files);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "查看云端文件详情")
    @GetMapping("/{fileCode}")
    public Response<CloudFileDetailDTO> cloudFileDetail(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "文件编码") @PathVariable String fileCode) {
        try {
            CloudFile file = cloudFileQueryService.getCloudFile(agentCode, fileCode);
            return Response.success(agentHttpAssembler.toCloudFileDetailResponse(file));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "读取云端文件内容")
    @GetMapping("/{fileCode}/content")
    public Response<String> getCloudFileContent(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "文件编码") @PathVariable String fileCode) {
        try {
            String content = cloudFileQueryService.getCloudFileContent(fileCode);
            return Response.success(content);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "手动触发备份", description = "将本地文件系统中变更过的文件上传到 OSS 备份")
    @PostMapping("/backup")
    public Response<Map<String, Object>> backup(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            BackupService.BackupStats stats = backupService.backupAgent(agentCode);
            return Response.success(Map.of(
                    "backedUpCount", stats.backedUpCount(),
                    "skippedCount", stats.skippedCount(),
                    "failedCount", stats.failedCount()));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "从备份恢复", description = "从 OSS 备份拉取文件到本地，用于新设备或灾难恢复")
    @PostMapping("/restore")
    public Response<Map<String, Object>> restore(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode) {
        try {
            int restoredCount = restoreService.restoreFromBackup(agentCode);
            return Response.success(Map.of("restoredCount", restoredCount));
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }
}
