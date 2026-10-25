package com.example.uet.user.repository;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Repository;

import com.example.uet.user.model.User;

/**
 * Simple in-memory user store.
 */
@Repository
public class UserRepository {

    private final Map<Long, User> users = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong();

    public Collection<User> findAll() {
        return users.values();
    }

    public Optional<User> findById(Long id) {
        return Optional.ofNullable(users.get(id));
    }

    public User save(String name, String email) {
        long id = idSequence.incrementAndGet();
        User user = new User(id, name, email, null);
        users.put(id, user);
        return user;
    }

    public User updateAvatar(Long id, String avatarUrl) {
        return users.computeIfPresent(id, (key, user) -> new User(user.id(), user.name(), user.email(), avatarUrl));
    }
}
