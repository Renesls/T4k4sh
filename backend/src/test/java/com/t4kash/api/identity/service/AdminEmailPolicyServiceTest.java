package com.t4kash.api.identity.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdminEmailPolicyServiceTest {
    @Test
    void recognizesConfiguredEmailsIgnoringCaseAndSpaces() {
        AdminEmailPolicyService policy = new AdminEmailPolicyService(
                " admin@t4kash.app, Evaluador@Example.com "
        );

        assertThat(policy.isAdmin("ADMIN@t4kash.app")).isTrue();
        assertThat(policy.isAdmin("evaluador@example.com")).isTrue();
        assertThat(policy.isAdmin("user@example.com")).isFalse();
    }

    @Test
    void keepsAdminAccessDisabledWithoutConfiguration() {
        AdminEmailPolicyService policy = new AdminEmailPolicyService("");

        assertThat(policy.isAdmin("admin@t4kash.app")).isFalse();
        assertThat(policy.isAdmin(null)).isFalse();
    }
}
