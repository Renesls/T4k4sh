package com.t4kash.api.identity.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SessionTokenServiceTest {
    private final SessionTokenService service = new SessionTokenService();

    @Test
    void generatedTokensAreUniqueAndUrlSafe() {
        String first = service.generateToken();
        String second = service.generateToken();

        assertThat(first)
                .isNotEqualTo(second)
                .hasSize(43)
                .matches("[A-Za-z0-9_-]+");
    }

    @Test
    void hashIsDeterministicWithoutExposingToken() {
        String token = "session-token";

        assertThat(service.hash(token))
                .isEqualTo(service.hash(token))
                .isNotEqualTo(token)
                .hasSize(64);
    }
}
