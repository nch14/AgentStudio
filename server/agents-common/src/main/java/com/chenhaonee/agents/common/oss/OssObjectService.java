package com.chenhaonee.agents.common.oss;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * OSS 对象存储读写封装。
 */
@Service
@Slf4j
public class OssObjectService {

    @Value("${oss.bucket:agents}")
    private String bucket;

    private final S3Client s3Client;
    private final AtomicBoolean bucketPrepared = new AtomicBoolean(false);

    public OssObjectService(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * 上传字节内容到 OSS。
     */
    public void uploadBytes(byte[] content, String objectKey, String contentType) {
        ensureBucket();
        try {
            PutObjectRequest.Builder builder = PutObjectRequest.builder()
                    .bucket(bucket)
                    .key(objectKey);
            if (contentType != null) {
                builder.contentType(contentType);
            }
            s3Client.putObject(builder.build(), RequestBody.fromBytes(content));
            log.info("上传对象成功, bucket={}, key={}", bucket, objectKey);
        } catch (Exception e) {
            throw new RuntimeException("上传对象到 OSS 失败: " + e.getMessage(), e);
        }
    }

    /**
     * 从 OSS 读取文本内容。
     */
    public String readAsString(String objectKey) {
        ensureBucket();
        try {
            byte[] data = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build())
                    .asByteArray();
            return new String(data, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("读取 OSS 对象失败, key=" + objectKey + ", cause=" + e.getMessage(), e);
        }
    }

    /**
     * 删除 OSS 对象。
     */
    public void delete(String objectKey) {
        ensureBucket();
        try {
            s3Client.deleteObject(b -> b.bucket(bucket).key(objectKey));
        } catch (Exception e) {
            throw new RuntimeException("删除 OSS 对象失败, key=" + objectKey + ", cause=" + e.getMessage(), e);
        }
    }

    /**
     * 按前缀列举 OSS 对象。
     */
    public List<String> listObjectsByPrefix(String prefix) {
        ensureBucket();
        try {
            ListObjectsV2Request request = ListObjectsV2Request.builder()
                    .bucket(bucket)
                    .prefix(prefix)
                    .build();
            ListObjectsV2Response response = s3Client.listObjectsV2(request);
            return response.contents().stream()
                    .map(S3Object::key)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            throw new RuntimeException("列举 OSS 对象失败, prefix=" + prefix + ", cause=" + e.getMessage(), e);
        }
    }

    /**
     * 检查 OSS 对象是否存在。
     */
    public boolean exists(String objectKey) {
        ensureBucket();
        try {
            s3Client.headObject(b -> b.bucket(bucket).key(objectKey));
            return true;
        } catch (S3Exception e) {
            if (e.statusCode() == 404 || e.statusCode() == 403) {
                return false;
            }
            throw e;
        }
    }

    /**
     * 检查并确保 bucket 存在。
     */
    public void ensureBucket() {
        if (bucketPrepared.get()) {
            return;
        }

        try {
            s3Client.headBucket(HeadBucketRequest.builder().bucket(bucket).build());
            bucketPrepared.set(true);
            return;
        } catch (NoSuchBucketException e) {
            // go create
        } catch (S3Exception e) {
            // 404: bucket not found; 301: MinIO may return redirect, treat as bucket exists
            if (e.statusCode() != 404 && e.statusCode() != 301) {
                throw new RuntimeException("检查 OSS bucket 失败: " + e.getMessage(), e);
            }
            // 301 means bucket exists but SDK got a redirect — mark as prepared
            if (e.statusCode() == 301) {
                bucketPrepared.set(true);
                return;
            }
        } catch (Exception e) {
            throw new RuntimeException("检查 OSS bucket 失败: " + e.getMessage(), e);
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
            log.info("OSS bucket自动创建成功，bucket={}", bucket);
            bucketPrepared.set(true);
        } catch (Exception e) {
            throw new RuntimeException("检查/创建 OSS bucket 失败: " + e.getMessage(), e);
        }
    }
}
