package com.t4kash.api.identity.service;

import com.t4kash.api.identity.dto.UserProfileResponse;
import com.t4kash.api.identity.entity.User;
import com.t4kash.api.identity.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class ProfileService {

    private final UserRepository userRepository;

    public ProfileService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserProfileResponse getUserProfile(Long userId) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuario no encontrado en la base de datos");
        }

        User user = userOpt.get();

            return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getFullName()
        );
    }
}