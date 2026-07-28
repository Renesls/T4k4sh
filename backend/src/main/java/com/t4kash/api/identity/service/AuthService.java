package com.t4kash.api.identity.service;

import com.t4kash.api.identity.dto.AuthRequest;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.RegisterRequest;
import com.t4kash.api.identity.entity.User;
import com.t4kash.api.identity.entity.UserSession;
import com.t4kash.api.identity.repository.UserRepository;
import com.t4kash.api.identity.repository.UserSessionRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository sessionRepository;

    public AuthService(UserRepository userRepository, UserSessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
    }

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists");
        }

        User newUser = new User();
        newUser.setFullName(request.getFullName());
        newUser.setEmail(request.getEmail());
        newUser.setPassword(request.getPassword()); // En un proyecto real esto va encriptado con BCrypt

        userRepository.save(newUser);

        return new AuthResponse(null, "User registered successfully");
    }

    public AuthResponse login(AuthRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());

        if (userOpt.isPresent() && userOpt.get().getPassword().equals(request.getPassword())) {
            String token = UUID.randomUUID().toString(); // Token rápido para el MVP

            UserSession session = new UserSession();
            session.setUser(userOpt.get());
            session.setToken(token);
            session.setExpirationDate(LocalDateTime.now().plusDays(1));
            session.setActive(true);

            sessionRepository.save(session);

            return new AuthResponse(token, "Login successful");
        }

        throw new RuntimeException("Invalid credentials");
    }
}