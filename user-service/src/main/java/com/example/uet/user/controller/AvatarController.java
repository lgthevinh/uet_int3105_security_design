package com.example.uet.user.controller;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.uet.user.dto.AvatarUploadForm;
import com.example.uet.user.dto.AvatarUploadRequest;
import com.example.uet.user.dto.ConfirmAvatarRequest;
import com.example.uet.user.model.User;
import com.example.uet.user.service.AvatarService;

/**
 * Avatar endpoints for the calling user.
 * The user id comes only from X-User-Id, which the gateway sets from the verified JWT.
 */
@RestController
@RequestMapping("/users/me/avatar")
public class AvatarController {

    private final AvatarService avatarService;

    public AvatarController(AvatarService avatarService) {
        this.avatarService = avatarService;
    }

    @PostMapping("/upload-form")
    public AvatarUploadForm createUploadForm(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody AvatarUploadRequest request) {
        return avatarService.createUploadForm(userId, request.contentType());
    }

    @PutMapping
    public User confirm(@RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ConfirmAvatarRequest request) {
        return avatarService.confirm(userId, request.objectKey());
    }
}
