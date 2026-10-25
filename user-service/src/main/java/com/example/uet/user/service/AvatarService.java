package com.example.uet.user.service;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.uet.user.config.MinioProperties;
import com.example.uet.user.dto.AvatarUploadForm;
import com.example.uet.user.exception.BadRequestException;
import com.example.uet.user.model.User;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PostPolicy;
import io.minio.RemoveObjectArgs;
import io.minio.SetBucketPolicyArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;

/**
 * Avatar upload using the Valet Key pattern.
 * This service only signs a short-lived POST policy; the client sends the image bytes straight to MinIO,
 * so large uploads never pass through the user service.
 */
@Service
public class AvatarService {

    // Only raster formats: SVG is left out because it can carry scripts and avatars are public-read
    private static final Map<String, String> ALLOWED_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp");

    private static final Pattern OBJECT_KEY = Pattern.compile("^(\\d+)/[0-9a-f-]{36}\\.(jpg|png|webp)$");

    private final MinioClient minio;
    private final MinioProperties properties;
    private final UserService userService;

    private volatile boolean bucketReady;

    public AvatarService(MinioClient minio, MinioProperties properties, UserService userService) {
        this.minio = minio;
        this.properties = properties;
        this.userService = userService;
    }

    /**
     * Signs a POST form that only allows this user to upload one object, at a fixed key,
     * with the requested content type and within the size limit. MinIO enforces all of these.
     */
    public AvatarUploadForm createUploadForm(Long userId, String contentType) {
        String extension = ALLOWED_TYPES.get(contentType);
        if (extension == null) {
            throw new BadRequestException("Content type must be one of " + ALLOWED_TYPES.keySet());
        }
        userService.getById(userId);
        ensureBucket();

        String objectKey = userId + "/" + UUID.randomUUID() + "." + extension;
        ZonedDateTime expiresAt = ZonedDateTime.now(ZoneOffset.UTC).plus(properties.uploadTtl());

        PostPolicy policy = new PostPolicy(properties.bucket(), expiresAt);
        policy.addEqualsCondition("key", objectKey);
        policy.addEqualsCondition("Content-Type", contentType);
        policy.addContentLengthRangeCondition(1, properties.maxAvatarSize().toBytes());

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("key", objectKey);
        fields.put("Content-Type", contentType);
        try {
            fields.putAll(minio.getPresignedPostFormData(policy));
        } catch (Exception e) {
            throw new IllegalStateException("Cannot sign avatar upload form", e);
        }

        return new AvatarUploadForm(bucketUrl(), fields, objectKey, expiresAt.toInstant());
    }

    /**
     * Sets the user's avatar to an object they uploaded.
     * Needed because the upload goes straight to MinIO, so this service only learns about it when the client confirms.
     * The object is checked again here as a second line of defence behind the POST policy.
     */
    public User confirm(Long userId, String objectKey) {
        var matcher = OBJECT_KEY.matcher(objectKey);
        if (!matcher.matches() || !matcher.group(1).equals(String.valueOf(userId))) {
            throw new BadRequestException("Object key does not belong to this user");
        }

        StatObjectResponse stat = stat(objectKey);
        if (stat.size() > properties.maxAvatarSize().toBytes() || !ALLOWED_TYPES.containsKey(stat.contentType())) {
            remove(objectKey);
            throw new BadRequestException("Uploaded avatar is too large or not an allowed image type");
        }

        return userService.updateAvatar(userId, bucketUrl() + "/" + objectKey);
    }

    private String bucketUrl() {
        return properties.publicUrl() + "/" + properties.bucket();
    }

    /**
     * Get Object metadata, in this case is avatar image
     * Needed for validation when user confirm to set the avatar image
     */
    private StatObjectResponse stat(String objectKey) {
        try {
            return minio.statObject(StatObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
        } catch (ErrorResponseException e) {
            if ("NoSuchKey".equals(e.errorResponse().code())) {
                throw new BadRequestException("Avatar has not been uploaded");
            }
            throw new IllegalStateException("Cannot check uploaded avatar", e);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot check uploaded avatar", e);
        }
    }

    private void remove(String objectKey) {
        try {
            minio.removeObject(RemoveObjectArgs.builder().bucket(properties.bucket()).object(objectKey).build());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot remove rejected avatar", e);
        }
    }

    /**
     * Creates the bucket with a public-read policy on first use.
     * Done lazily instead of at startup because MinIO may not be ready when this service boots.
     */
    private synchronized void ensureBucket() {
        if (bucketReady) {
            return;
        }
        String bucket = properties.bucket();
        // Anonymous users may only read objects, not list or write them
        String publicReadPolicy = """
                {"Version":"2012-10-17","Statement":[{"Effect":"Allow","Principal":{"AWS":["*"]},\
                "Action":["s3:GetObject"],"Resource":["arn:aws:s3:::%s/*"]}]}""".formatted(bucket);
        try {
            if (!minio.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minio.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
            minio.setBucketPolicy(SetBucketPolicyArgs.builder().bucket(bucket).config(publicReadPolicy).build());
        } catch (Exception e) {
            throw new IllegalStateException("Cannot prepare avatar bucket", e);
        }
        bucketReady = true;
    }
}
