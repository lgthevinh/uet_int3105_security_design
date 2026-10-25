package com.example.uet.user.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Presigned POST form for uploading an avatar straight to MinIO.
 * The client sends every entry of fields as a multipart form field, then the image as the last field named "file".
 */
public record AvatarUploadForm(String url, Map<String, String> fields, String objectKey, Instant expiresAt) {
}
