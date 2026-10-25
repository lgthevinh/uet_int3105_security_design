package com.example.uet.user.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmAvatarRequest(@NotBlank String objectKey) {
}
