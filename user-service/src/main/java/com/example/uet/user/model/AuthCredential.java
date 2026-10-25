package com.example.uet.user.model;

/**
 * Login credential for a user.
 * Kept separate from User so the password never ends up in /users responses.
 * Password is plain text for now; hashing will be added later.
 */
public record AuthCredential(Long userId, String email, String password) {
}
