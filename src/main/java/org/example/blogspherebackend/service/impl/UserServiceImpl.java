package org.example.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.example.blogspherebackend.entity.User;
import org.example.blogspherebackend.exception.ResourceNotFoundException;
import org.example.blogspherebackend.repository.UserRepository;
import org.example.blogspherebackend.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Implementation of UserService interface.
 * Provides business logic for user management operations.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User createUser(User user) {
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmailAndIsDeletedFalse(email);
    }

    @Override
    public Optional<User> findById(Long id) {
        return userRepository.findById(id)
                .filter(user -> !user.getIsDeleted());
    }

    @Override
    @Transactional
    public User updateUser(Long id, User updatedUser) {
        User existingUser = userRepository.findById(id)
                .filter(user -> !user.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (updatedUser.getName() != null) {
            existingUser.setName(updatedUser.getName());
        }
        if (updatedUser.getBio() != null) {
            existingUser.setBio(updatedUser.getBio());
        }
        if (updatedUser.getAvatarUrl() != null) {
            existingUser.setAvatarUrl(updatedUser.getAvatarUrl());
        }

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        user.setIsDeleted(true);
        userRepository.save(user);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmailAndIsDeletedFalse(email);
    }
}

