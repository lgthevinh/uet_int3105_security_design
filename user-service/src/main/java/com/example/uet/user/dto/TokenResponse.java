package com.example.uet.user.dto;

public record TokenResponse(String token, String tokenType, long expiresIn) {
}
