package com.t4kash.api.identity.service;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class VerificationCodeService {
    private static final int CODE_LIMIT = 1_000_000;
    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        return "%06d".formatted(secureRandom.nextInt(CODE_LIMIT));
    }
}
