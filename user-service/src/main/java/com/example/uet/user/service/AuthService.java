package com.example.uet.user.service;

import org.springframework.stereotype.Service;

import com.example.uet.user.dto.CreateUserRequest;
import com.example.uet.user.dto.LoginRequest;
import com.example.uet.user.dto.RegisterRequest;
import com.example.uet.user.dto.TokenResponse;
import com.example.uet.user.exception.ConflictException;
import com.example.uet.user.exception.UnauthorizedException;
import com.example.uet.user.model.Credential;
import com.example.uet.user.model.User;
import com.example.uet.user.repository.CredentialRepository;

@Service
public class AuthService {

    private final UserService userService;
    private final CredentialRepository credentialRepository;
    private final JwtService jwtService;

    public AuthService(UserService userService, CredentialRepository credentialRepository, JwtService jwtService) {
        this.userService = userService;
        this.credentialRepository = credentialRepository;
        this.jwtService = jwtService;
    }

    /**
     * Creates the user profile and its login credential together,
     * so every registered user can log in.
     */
    public User register(RegisterRequest request) {
        if (credentialRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email " + request.email() + " is already registered");
        }
        User user = userService.create(new CreateUserRequest(request.name(), request.email()));
        credentialRepository.save(new Credential(user.id(), request.email(), request.password()));
        return user;
    }

    public TokenResponse login(LoginRequest request) {
        // Same error for unknown email and wrong password, so callers can't probe which emails exist
        Credential credential = credentialRepository.findByEmail(request.email())
                .filter(c -> c.password().equals(request.password()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return new TokenResponse(jwtService.issue(credential.userId()), "Bearer", jwtService.ttlSeconds());
    }
}
