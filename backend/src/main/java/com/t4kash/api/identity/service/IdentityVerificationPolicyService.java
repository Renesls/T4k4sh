package com.t4kash.api.identity.service;

import com.t4kash.api.exception.AccountNotVerifiedException;
import com.t4kash.api.identity.entity.VerificacionIdentidad;
import com.t4kash.api.identity.repository.VerificacionIdentidadRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class IdentityVerificationPolicyService {
    private final VerificacionIdentidadRepository verificationRepository;

    public IdentityVerificationPolicyService(
            VerificacionIdentidadRepository verificationRepository
    ) {
        this.verificationRepository = verificationRepository;
    }

    @Transactional(readOnly = true)
    public boolean isApproved(Integer userId) {
        return verificationRepository
                .findFirstByIdUsuarioOrderByFechaInicioDesc(userId)
                .map(this::isApprovedNow)
                .orElse(false);
    }

    @Transactional(readOnly = true)
    public void requireApproved(Integer userId, String action) {
        if (!isApproved(userId)) {
            throw new AccountNotVerifiedException(
                    "Verifica tu identidad desde Perfil antes de " + action + "."
            );
        }
    }

    boolean isApprovedNow(VerificacionIdentidad verification) {
        return "APROBADA".equals(verification.getEstadoVerificacion())
                && (verification.getFechaExpiracion() == null
                    || verification.getFechaExpiracion().isAfter(LocalDateTime.now()));
    }
}
