package com.chenhaonee.agents.app.interfaces.http.oss;

import com.chenhaonee.agents.common.utils.OssUploadUtils;
import com.chenhaonee.agents.common.utils.UrlUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 提供前端直传 OSS 的预签名上传地址。
 */
@CrossOrigin
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class OssFileController {

    private final S3Presigner presigner;

    @Value("${oss.bucket:agents}")
    private String bucket;

    @Value("${oss.public-url:${oss.url}}")
    private String ossPublicUrl;

    @Value("${oss.presign-expire-minutes:10}")
    private long presignExpireMinutes;

    @Schema(name = "OssTarget", description = "上传地址申请结果")
    public record OssTarget(
            @Schema(description = "可直接用于 PUT 上传的预签名 URL")
            String uploadUrl,
            @Schema(description = "对象 key，用于业务侧持久化")
            String objectKey,
            @Schema(description = "对象访问 URL")
            String publicUrl,
            @Schema(description = "上传时建议使用的 Content-Type")
            String contentType) {
    }

    /**
     * 申请预签名上传地址。
     */
    @GetMapping("/upload-url")
    @Operation(summary = "申请 OSS 上传地址", description = "按文件名与可选目录生成对象 key，并返回可直接 PUT 的预签名地址")
    public OssTarget uploadUrl(
            @Parameter(description = "原始文件名", example = "poster.jpg")
            @RequestParam String filename,
            @Parameter(description = "文件类型，不传则按扩展名推断", example = "image/jpeg")
            @RequestParam(required = false) String contentType,
            @Parameter(description = "业务子目录（可选）", example = "user/1001/avatar")
            @RequestParam(required = false, defaultValue = "") String dir,
            @Parameter(description = "关联的 Agent 编码（可选）")
            @RequestParam(required = false) String agentCode) {
        if (StringUtils.isBlank(filename)) {
            throw new IllegalArgumentException("filename 不能为空");
        }

        String safeName = OssUploadUtils.sanitizeFileName(filename);
        String safeDir = OssUploadUtils.sanitizeDir(dir);
        String safeContentType = StringUtils.isBlank(contentType)
                ? OssUploadUtils.guessContentType(safeName)
                : contentType.trim();
        String typePrefix = OssUploadUtils.chooseTypePrefix(safeContentType, safeName);
        String date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String subPath = StringUtils.isBlank(safeDir) ? "" : safeDir + "/";
        String agentPrefix = StringUtils.isBlank(agentCode) ? "" : agentCode + "/";
        String key = agentPrefix + typePrefix + "/" + date + "/" + subPath + UUID.randomUUID() + "-" + safeName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(safeContentType)
                .build();

        var presigned = presigner.presignPutObject(b -> b
                .putObjectRequest(putObjectRequest)
                .signatureDuration(resolveSignatureDuration())
        );

        String objectUrl = UrlUtils.joinHttpRootAndPath(ossPublicUrl, "/" + bucket + "/" + key);
        log.info("申请上传签名成功, key={}, bucket={}, expireMinutes={}", key, bucket, presignExpireMinutes);
        return new OssTarget(presigned.url().toString(), key, objectUrl, safeContentType);
    }

    private Duration resolveSignatureDuration() {
        long safeMinutes = Math.max(1, Math.min(presignExpireMinutes, 7 * 24 * 60));
        return Duration.ofMinutes(safeMinutes);
    }
}
