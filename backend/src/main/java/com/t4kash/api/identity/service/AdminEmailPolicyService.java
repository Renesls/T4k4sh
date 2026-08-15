package com.t4kash.api.identity.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AdminEmailPolicyService {
    private final Set<String> adminEmails;

    public AdminEmailPolicyService(
            @Value("${app.auth.admin-emails:}") String configuredEmails
    ) {
        adminEmails = Arrays.stream(configuredEmails.split(","))
                .map(String::trim)
                .map(value -> value.toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    public boolean isAdmin(String email) {
        return email != null
                && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }
}
