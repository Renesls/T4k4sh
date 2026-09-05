package com.t4kash.api.identity.dto;

import java.util.UUID;

public record IdentityVerificationSessionResponse(
        UUID idSesionProveedor,
        String urlVerificacion,
        String estado
) { }
