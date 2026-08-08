package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.entity.DominioUniversidad;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.DominioUniversidadRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class RegistrationPolicyService {
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final DominioUniversidadRepository dominioRepository;
    private final Set<String> evaluatorEmails;

    public RegistrationPolicyService(
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            DominioUniversidadRepository dominioRepository,
            @Value("${app.auth.evaluator-emails:}") String evaluatorEmails
    ) {
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.dominioRepository = dominioRepository;
        this.evaluatorEmails = parseEmails(evaluatorEmails);
    }

    public RegistrationProfile resolve(
            String normalizedEmail,
            Integer universityId,
            Integer careerId
    ) {
        boolean evaluator = evaluatorEmails.contains(normalizedEmail);
        if (universityId == null) {
            if (careerId != null) {
                throw new IllegalArgumentException(
                        "No puedes seleccionar una carrera sin universidad."
                );
            }
            if (!evaluator && belongsToRegisteredUniversity(normalizedEmail)) {
                throw new IllegalArgumentException(
                        "Este correo es institucional. Selecciona tu universidad y carrera."
                );
            }
            return new RegistrationProfile(evaluator, evaluator, null, null);
        }
        if (careerId == null) {
            throw new IllegalArgumentException(
                    "Selecciona una carrera para completar el perfil estudiantil."
            );
        }

        Universidad university = universidadRepository
                .findByIdUniversidadAndEstadoTrue(universityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La universidad seleccionada no existe o esta inactiva."
                ));
        carreraRepository.findByIdCarreraAndIdUniversidadAndEstadoTrue(
                careerId,
                university.getIdUniversidad()
        ).orElseThrow(() -> new IllegalArgumentException(
                "La carrera no pertenece a la universidad seleccionada."
        ));
        DominioUniversidad registeredDomain = dominioRepository
                .findByDominioCorreoIgnoreCaseAndEstadoTrue(emailDomain(normalizedEmail))
                .orElse(null);
        if (!evaluator && registeredDomain != null
                && !registeredDomain.getIdUniversidad().equals(university.getIdUniversidad())) {
            throw new IllegalArgumentException(
                "El correo no pertenece al dominio de la universidad seleccionada."
            );
        }
        boolean automaticAccess = evaluator || (
                registeredDomain != null
                        && registeredDomain.getIdUniversidad().equals(
                                university.getIdUniversidad()
                        )
                        && registeredDomain.isVerificacionAutomatica()
        );
        return new RegistrationProfile(
                true,
                automaticAccess,
                university,
                careerId
        );
    }

    private boolean belongsToRegisteredUniversity(String email) {
        return dominioRepository
                .findByDominioCorreoIgnoreCaseAndEstadoTrue(emailDomain(email))
                .isPresent();
    }

    private String emailDomain(String email) {
        return email.substring(email.lastIndexOf('@') + 1)
                .toLowerCase(Locale.ROOT);
    }

    private Set<String> parseEmails(String configuredEmails) {
        return Arrays.stream(configuredEmails.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
