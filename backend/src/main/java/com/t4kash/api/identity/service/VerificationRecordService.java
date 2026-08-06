package com.t4kash.api.identity.service;

import com.t4kash.api.identity.entity.VerificacionUsuario;
import com.t4kash.api.identity.repository.VerificacionUsuarioRepository;
import com.t4kash.api.marketplace.entity.Usuario;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Creates VerificacionUsuario records. Shared by RegistrationService (email
 * verification) and LoginService (2FA challenges) so the record shape stays
 * identical between both flows.
 */
@Service
public class VerificationRecordService {
    private static final String PENDING_VERIFICATION = "PENDIENTE";

    private final VerificacionUsuarioRepository verificacionRepository;
    private final VerificationCodeService codeService;

    public VerificationRecordService(
            VerificacionUsuarioRepository verificacionRepository,
            VerificationCodeService codeService
    ) {
        this.verificacionRepository = verificacionRepository;
        this.codeService = codeService;
    }

    public VerificacionUsuario create(
            Usuario usuario,
            LocalDateTime now,
            String verificationType,
            int expirationMinutes
    ) {
        VerificacionUsuario verification = new VerificacionUsuario();
        verification.setIdUsuario(usuario.getIdUsuario());
        verification.setCorreoInstitucional(usuario.getCorreo());
        verification.setCodigoVerificacion(codeService.generate());
        verification.setEstadoVerificacion(PENDING_VERIFICATION);
        verification.setFechaSolicitud(now);
        verification.setFechaExpiracion(now.plusMinutes(expirationMinutes));
        verification.setTipoVerificacion(verificationType);
        return verificacionRepository.save(verification);
    }
}
