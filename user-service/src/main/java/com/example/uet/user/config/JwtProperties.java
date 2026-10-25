package com.example.uet.user.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT settings bound from the jwt.* properties.
 * The secret must match the gateway's, since nginx verifies the tokens this service signs.
 */
@ConfigurationProperties(prefix = "jwt")
public record JwtProperties(String secret, Duration ttl) {
}
