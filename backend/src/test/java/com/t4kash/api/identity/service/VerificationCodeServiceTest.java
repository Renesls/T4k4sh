package com.t4kash.api.identity.service;

import org.junit.jupiter.api.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class VerificationCodeServiceTest {
    private final VerificationCodeService service = new VerificationCodeService();

    @Test
    void generatesSixDigitCodes() {
        IntStream.range(0, 100)
                .mapToObj(ignored -> service.generate())
                .forEach(code -> assertThat(code).matches("\\d{6}"));
    }

    @Test
    void generatesDifferentValues() {
        long distinctCodes = IntStream.range(0, 25)
                .mapToObj(ignored -> service.generate())
                .distinct()
                .count();

        assertThat(distinctCodes).isGreaterThan(1);
    }
}
