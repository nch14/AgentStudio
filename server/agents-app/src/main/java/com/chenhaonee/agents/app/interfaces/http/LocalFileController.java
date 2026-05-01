package com.chenhaonee.agents.app.interfaces.http;

import com.chenhaonee.agents.app.application.agent.LocalFileApplicationService;
import com.chenhaonee.agents.app.application.agent.LocalFileApplicationService.LocalFileEntry;
import com.chenhaonee.agents.app.interfaces.http.agent.AgentHttpAssembler;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.AgentFileEntryDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.LocalFileCreateRequest;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.LocalFileDetailDTO;
import com.chenhaonee.agents.app.interfaces.http.agent.dto.LocalFileWriteContentRequest;
import com.chenhaonee.agents.app.interfaces.http.common.ExceptionHandlers;
import com.chenhaonee.agents.app.interfaces.http.common.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 本地文件浏览与编辑 API — 直接操作本地文件系统，无任何 DB 依赖。
 */
@Tag(name = "LocalFile", description = "Agent 本地文件浏览与编辑")
@RestController
@RequestMapping("/api/v1/agents/{agentCode}/local-files")
public class LocalFileController {

    private final LocalFileApplicationService localFileService;
    private final AgentHttpAssembler agentHttpAssembler;

    public LocalFileController(LocalFileApplicationService localFileService,
                               AgentHttpAssembler agentHttpAssembler) {
        this.localFileService = localFileService;
        this.agentHttpAssembler = agentHttpAssembler;
    }

    @Operation(summary = "列出 agent 的所有本地文件")
    @GetMapping
    public Response<List<LocalFileDetailDTO>> listFiles(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "限定路径，只返回该路径下的文件") @RequestParam(required = false) String path,
            @Parameter(description = "排除路径，过滤掉该路径下的文件") @RequestParam(required = false) String exclude) {
        try {
            List<LocalFileDetailDTO> files = localFileService.listFiles(agentCode, path, exclude).stream()
                    .map(agentHttpAssembler::toLocalFileDetailResponse)
                    .toList();
            return Response.success(files);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "类 ls 接口，按路径层级查询文件和目录")
    @GetMapping("/ls")
    public Response<List<AgentFileEntryDTO>> ls(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "目录路径") @RequestParam(required = false, defaultValue = "") String path) {
        try {
            String normalizedPath = path == null ? "" : path;
            if (!normalizedPath.isEmpty() && !normalizedPath.endsWith("/")) {
                normalizedPath += "/";
            }

            List<LocalFileEntry> files = localFileService.listFilesByPath(agentCode, normalizedPath);
            List<String> subPaths = localFileService.listSubPaths(agentCode, normalizedPath);

            List<AgentFileEntryDTO> entries = new ArrayList<>();
            for (LocalFileEntry file : files) {
                entries.add(agentHttpAssembler.toLocalFileEntryResponse(file));
            }

            for (String subPath : subPaths) {
                String relativePart = subPath.substring(normalizedPath.length());
                String[] segments = relativePart.split("/");
                if (segments.length > 0 && !segments[0].isEmpty()) {
                    String dirName = segments[0];
                    entries.add(AgentFileEntryDTO.directory(dirName, normalizedPath + dirName + "/"));
                }
            }

            return Response.success(entries);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "读取本地文件内容")
    @GetMapping("/content")
    public Response<String> readFile(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "文件逻辑路径") @RequestParam String path) {
        try {
            String content = localFileService.readFileByPathForContent(agentCode, path);
            return Response.success(content);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "创建本地文件")
    @PostMapping
    public Response<Void> createFile(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @RequestBody LocalFileCreateRequest request) {
        try {
            String logicalPath = buildLogicalPath(
                    request.path() == null || request.path().isBlank() ? "home/" : request.path(),
                    request.name());
            localFileService.createFile(agentCode, logicalPath, request.content() == null ? "" : request.content());
            return Response.success(null);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "覆盖写入本地文件内容")
    @PutMapping("/content")
    public Response<Void> writeFile(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @RequestBody LocalFileWriteContentRequest request) {
        try {
            localFileService.writeFile(agentCode, request.path(), request.content());
            return Response.success(null);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    @Operation(summary = "删除本地文件")
    @DeleteMapping
    public Response<Void> deleteFile(
            @Parameter(description = "Agent 编码") @PathVariable String agentCode,
            @Parameter(description = "文件逻辑路径") @RequestParam String path) {
        try {
            localFileService.deleteFile(agentCode, path);
            return Response.success(null);
        } catch (Exception e) {
            return Response.error(ExceptionHandlers.handleException(e));
        }
    }

    private String buildLogicalPath(String path, String name) {
        String normalizedPath = path.startsWith("/") ? path.substring(1) : path;
        normalizedPath = normalizedPath.endsWith("/") ? normalizedPath : normalizedPath + "/";
        return normalizedPath + name;
    }
}
