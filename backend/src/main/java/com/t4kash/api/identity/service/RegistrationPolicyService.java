package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
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
    private final Set<String> evaluatorEmails;

    public RegistrationPolicyService(
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            @Value("${app.auth.evaluator-emails:}") String evaluatorEmails
    ) {
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
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
        carreraRepository.findByIdCarreraAndIdUniversidad(
                careerId,
                university.getIdUniversidad()
        ).orElseThrow(() -> new IllegalArgumentException(
                "La carrera no pertenece a la universidad seleccionada."
        ));
        boolean universityHasDomain = university.getDominioCorreo() != null
                && !university.getDominioCorreo().isBlank();
        if (!evaluator && universityHasDomain && !emailDomain(normalizedEmail).equals(
                normalizeDomain(university.getDominioCorreo())
        )) {
            throw new IllegalArgumentException(
                    "El correo no pertenece al dominio de la universidad seleccionada."
            );
        }
        return new RegistrationProfile(
                true,
                evaluator || universityHasDomain,
                university,
                careerId
        );
    }

    private boolean belongsToRegisteredUniversity(String email) {
        String domain = emailDomain(email);
        return universidadRepository.findAllByEstadoTrueOrderByNombreUniversidad()
                .stream()
                .map(Universidad::getDominioCorreo)
                .filter(value -> value != null && !value.isBlank())
                .map(this::normalizeDomain)
                .anyMatch(domain::equals);
    }

    private String emailDomain(String email) {
        return email.substring(email.lastIndexOf('@') + 1)
                .toLowerCase(Locale.ROOT);
    }

    private String normalizeDomain(String domain) {
        return domain.trim()
                .toLowerCase(Locale.ROOT)
                .replaceFirst("^@", "");
    }

    private Set<String> parseEmails(String configuredEmails) {
        return Arrays.stream(configuredEmails.split(","))
                .map(value -> value.trim().toLowerCase(Locale.ROOT))
                .filter(value -> !value.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }
}
