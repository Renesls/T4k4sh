package com.t4kash.api.identity.controller;

import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.AuthResponse;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.LoginRequest;
import com.t4kash.api.identity.dto.RegisterRequest;
import com.t4kash.api.identity.dto.RegistrationResponse;
import com.t4kash.api.identity.dto.ResendVerificationRequest;
import com.t4kash.api.identity.dto.VerifyEmailRequest;
import com.t4kash.api.identity.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final String BEARER_PREFIX = "Bearer ";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegistrationResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        RegistrationResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/verify-email")
    public AuthResponse verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.verifyEmail(
                request,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    @PostMapping("/resend-verification")
    public RegistrationResponse resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return authService.resendVerification(request.correo());
    }

    @PostMapping("/login")
    public AuthResponse login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest
    ) {
        return authService.login(
                request,
                clientIp(httpRequest),
                httpRequest.getHeader(HttpHeaders.USER_AGENT)
        );
    }

    @GetMapping("/me")
    public AuthenticatedUserResponse me(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        return authService.getCurrentUser(bearerToken(authorization));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false)
            String authorization
    ) {
        authService.logout(bearerToken(authorization));
        return ResponseEntity.noContent().build();
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith(BEARER_PREFIX)) {
            throw new InvalidCredentialsException("Debes iniciar sesion.");
        }
        return authorization.substring(BEARER_PREFIX.length()).trim();
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
