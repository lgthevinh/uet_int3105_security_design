package com.example.uet.user.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinioConfig {

    /**
     * Client used to sign upload forms and check uploaded objects.
     * The region is set explicitly so signing does not need a network call to MinIO.
     */
    @Bean
    public MinioClient minioClient(MinioProperties properties) {
        return MinioClient.builder()
                .endpoint(properties.endpoint())
                .credentials(properties.accessKey(), properties.secretKey())
                .region(properties.region())
                .build();
    }
}
