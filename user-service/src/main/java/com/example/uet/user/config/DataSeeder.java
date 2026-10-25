package com.example.uet.user.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.example.uet.user.dto.RegisterRequest;
import com.example.uet.user.service.AuthService;

/**
 * Seeds demo accounts at startup so login, the gateway and /dashboard can be tested right away.
 * Registration order matters: Alice gets id 1 and Bob id 2, matching the orders seeded in order-service.
 */
@Component
public class DataSeeder implements ApplicationRunner {

    private final AuthService authService;

    public DataSeeder(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public void run(ApplicationArguments args) {
        authService.register(new RegisterRequest("Alice", "alice@example.com", "password123"));
        authService.register(new RegisterRequest("Bob", "bob@example.com", "password123"));
    }
}
