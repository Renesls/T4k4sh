package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthenticatedUserService {
    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthSessionService authSessionService;

    public AuthenticatedUserService(AuthSessionService authSessionService) {
        this.authSessionService = authSessionService;
    }

    public AuthenticatedUserResponse requireUser(String authorization) {
        return authSessionService.getCurrentUser(resolveToken(authorization));
    }

    public AuthenticatedUserResponse requireRole(String authorization, String requiredRole) {
        AuthenticatedUserResponse user = requireUser(authorization);
        boolean hasRole = user.roles().stream()
                .anyMatch(role -> role.equalsIgnoreCase(requiredRole));
        if (!hasRole) {
            throw new ForbiddenOperationException(
                    "Tu cuenta no tiene permiso para realizar esta accion."
            );
        }
        return user;
    }

    private String resolveToken(String authorization) {
        if (
                authorization == null ||
                !authorization.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())
        ) {
            throw new InvalidCredentialsException("Debes iniciar sesion.");
        }
        String token = authorization.substring(BEARER_PREFIX.length()).trim();
        if (token.isBlank()) {
            throw new InvalidCredentialsException("Debes iniciar sesion.");
        }
        return token;
    }
}
