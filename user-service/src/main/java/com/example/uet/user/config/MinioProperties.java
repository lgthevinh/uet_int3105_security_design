package com.example.uet.user.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * MinIO settings bound from the minio.* properties.
 * endpoint is what this service calls inside Docker; publicUrl is what clients reach through the gateway.
 */
@ConfigurationProperties(prefix = "minio")
public record MinioProperties(
        String endpoint,
        String publicUrl,
        String accessKey,
        String secretKey,
        String region,
        String bucket,
        Duration uploadTtl,
        DataSize maxAvatarSize) {
}
