package com.chenhaonee.agents.app.interfaces.http.common.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * 附件请求体，仅携带元数据，不传二进制。
 * 前端先调 /api/files/upload-url 直传 OSS 后回填。
 */
@Schema(name = "AttachmentRequest", description = "附件元数据，仅引用 OSS 对象")
public record AttachmentRequest(
        @Schema(description = "OSS 对象 key", example = "agentX/image/2026-05-02/uuid-foo.png")
        @NotBlank String ossKey,

        @Schema(description = "MIME 类型", example = "image/png")
        @NotBlank String mime,

        @Schema(description = "原始文件名", example = "foo.png")
        @NotBlank String filename,

        @Schema(description = "文件大小（字节）", example = "12528")
        @Min(1) long size
) {
}
