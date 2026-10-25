package com.example.uet.user.repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

import com.example.uet.user.model.AuthCredential;

/**
 * In-memory credential store, keyed by email since login looks credentials up by email.
 */
@Repository
public class AuthCredentialRepository {

    private final Map<String, AuthCredential> credentialsByEmail = new ConcurrentHashMap<>();

    public Optional<AuthCredential> findByEmail(String email) {
        return Optional.ofNullable(credentialsByEmail.get(email));
    }

    public boolean existsByEmail(String email) {
        return credentialsByEmail.containsKey(email);
    }

    public AuthCredential save(AuthCredential credential) {
        credentialsByEmail.put(credential.email(), credential);
        return credential;
    }
}
