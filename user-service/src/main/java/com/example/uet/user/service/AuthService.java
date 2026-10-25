package com.example.uet.user.service;

import org.springframework.stereotype.Service;

import com.example.uet.user.dto.CreateUserRequest;
import com.example.uet.user.dto.LoginRequest;
import com.example.uet.user.dto.RegisterRequest;
import com.example.uet.user.dto.TokenResponse;
import com.example.uet.user.exception.ConflictException;
import com.example.uet.user.exception.UnauthorizedException;
import com.example.uet.user.model.AuthCredential;
import com.example.uet.user.model.User;
import com.example.uet.user.repository.AuthCredentialRepository;

@Service
public class AuthService {

    private final UserService userService;
    private final AuthCredentialRepository authCredentialRepository;
    private final JwtService jwtService;

    public AuthService(UserService userService, AuthCredentialRepository authCredentialRepository, JwtService jwtService) {
        this.userService = userService;
        this.authCredentialRepository = authCredentialRepository;
        this.jwtService = jwtService;
    }

    /**
     * Creates the user profile and its login credential together,
     * so every registered user can log in.
     */
    public User register(RegisterRequest request) {
        if (authCredentialRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email " + request.email() + " is already registered");
        }
        User user = userService.create(new CreateUserRequest(request.name(), request.email()));
        authCredentialRepository.save(new AuthCredential(user.id(), request.email(), request.password()));
        return user;
    }

    public TokenResponse login(LoginRequest request) {
        // Same error for unknown email and wrong password, so callers can't probe which emails exist
        AuthCredential credential = authCredentialRepository.findByEmail(request.email())
                .filter(c -> c.password().equals(request.password()))
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));
        return new TokenResponse(jwtService.issue(credential.userId()), "Bearer", jwtService.ttlSeconds());
    }
}
