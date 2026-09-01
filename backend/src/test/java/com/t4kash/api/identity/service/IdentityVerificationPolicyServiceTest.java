package com.t4kash.api.identity.service;

import com.t4kash.api.exception.AccountNotVerifiedException;
import com.t4kash.api.identity.entity.VerificacionIdentidad;
import com.t4kash.api.identity.repository.VerificacionIdentidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityVerificationPolicyServiceTest {
    @Mock
    private VerificacionIdentidadRepository verificationRepository;

    private IdentityVerificationPolicyService service;

    @BeforeEach
    void setUp() {
        service = new IdentityVerificationPolicyService(verificationRepository);
    }

    @Test
    void approvedCurrentIdentityAllowsProtectedOperation() {
        VerificacionIdentidad verification = verification("APROBADA");
        verification.setFechaExpiracion(LocalDateTime.now().plusDays(1));
        when(verificationRepository.findFirstByIdUsuarioOrderByFechaInicioDesc(1))
                .thenReturn(Optional.of(verification));

        assertDoesNotThrow(() -> service.requireApproved(1, "consultar Wallet"));
    }

    @Test
    void expiredApprovalBlocksProtectedOperation() {
        VerificacionIdentidad verification = verification("APROBADA");
        verification.setFechaExpiracion(LocalDateTime.now().minusMinutes(1));
        when(verificationRepository.findFirstByIdUsuarioOrderByFechaInicioDesc(1))
                .thenReturn(Optional.of(verification));

        assertThrows(
                AccountNotVerifiedException.class,
                () -> service.requireApproved(1, "consultar Wallet")
        );
    }

    @Test
    void missingIdentityBlocksProtectedOperation() {
        when(verificationRepository.findFirstByIdUsuarioOrderByFechaInicioDesc(1))
                .thenReturn(Optional.empty());

        assertThrows(
                AccountNotVerifiedException.class,
                () -> service.requireApproved(1, "aceptar una postulacion")
        );
    }

    private VerificacionIdentidad verification(String status) {
        VerificacionIdentidad verification = new VerificacionIdentidad();
        verification.setEstadoVerificacion(status);
        return verification;
    }
}
