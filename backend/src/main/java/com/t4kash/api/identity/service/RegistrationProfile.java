package com.t4kash.api.identity.service;

import com.t4kash.api.identity.entity.Universidad;

public record RegistrationProfile(
        boolean studentRequested,
        boolean automaticStudentAccess,
        Universidad university,
        Integer careerId
) {
    public boolean student() {
        return studentRequested;
    }
}
