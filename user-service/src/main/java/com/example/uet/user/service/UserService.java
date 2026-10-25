package com.example.uet.user.service;

import java.util.Collection;

import org.springframework.stereotype.Service;

import com.example.uet.user.dto.CreateUserRequest;
import com.example.uet.user.exception.NotFoundException;
import com.example.uet.user.model.User;
import com.example.uet.user.repository.UserRepository;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Collection<User> getAll() {
        return userRepository.findAll();
    }

    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User " + id + " not found"));
    }

    public User create(CreateUserRequest request) {
        return userRepository.save(request.name(), request.email());
    }

    public User updateAvatar(Long id, String avatarUrl) {
        User user = userRepository.updateAvatar(id, avatarUrl);
        if (user == null) {
            throw new NotFoundException("User " + id + " not found");
        }
        return user;
    }
}
