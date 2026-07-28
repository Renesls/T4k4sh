package com.t4kash.api.identity.service;

import com.t4kash.api.identity.entity.Universidad;

public record RegistrationProfile(
        boolean student,
        Universidad university,
        Integer careerId
) {
}
