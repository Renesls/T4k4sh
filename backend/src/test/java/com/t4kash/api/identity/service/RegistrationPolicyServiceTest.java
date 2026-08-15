package com.t4kash.api.identity.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.t4kash.api.identity.entity.Carrera;
import com.t4kash.api.identity.entity.DominioUniversidad;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.DominioUniversidadRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class RegistrationPolicyServiceTest {
    @Mock
    private UniversidadRepository universidadRepository;

    @Mock
    private CarreraRepository carreraRepository;

    @Mock
    private DominioUniversidadRepository dominioRepository;

    @Test
    void normalEmailCreatesClientProfile() {
        when(dominioRepository.findByDominioCorreoIgnoreCaseAndEstadoTrue("gmail.com"))
                .thenReturn(Optional.empty());

        RegistrationProfile profile = service("").resolve(
                "client@gmail.com",
                null,
                null
        );

        assertFalse(profile.student());
        assertNull(profile.university());
        assertNull(profile.careerId());
    }

    @Test
    void institutionalEmailMustSelectUniversityAndCareer() {
        DominioUniversidad registeredDomain = mock(DominioUniversidad.class);
        when(dominioRepository.findByDominioCorreoIgnoreCaseAndEstadoTrue("uamv.edu.ni"))
                .thenReturn(Optional.of(registeredDomain));

        assertThrows(
                IllegalArgumentException.class,
                () -> service("").resolve("student@uamv.edu.ni", null, null)
        );
    }

    @Test
    void verifiedUniversitySelectionCreatesStudentProfile() {
        Universidad university = university(1);
        DominioUniversidad registeredDomain = domain(1, true);
        when(universidadRepository.findByIdUniversidadAndEstadoTrue(1))
                .thenReturn(Optional.of(university));
        when(carreraRepository.findByIdCarreraAndIdUniversidadAndEstadoTrue(4, 1))
                .thenReturn(Optional.of(mock(Carrera.class)));
        when(dominioRepository.findByDominioCorreoIgnoreCaseAndEstadoTrue("uamv.edu.ni"))
                .thenReturn(Optional.of(registeredDomain));

        RegistrationProfile profile = service("").resolve(
                "student@uamv.edu.ni",
                1,
                4
        );

        assertTrue(profile.student());
        assertTrue(profile.university() == university);
        assertTrue(profile.careerId() == 4);
    }

    @Test
    void evaluatorReceivesStudentCapabilitiesWithoutInstitutionalDomain() {
        RegistrationProfile profile = service("evaluator@gmail.com").resolve(
                "evaluator@gmail.com",
                null,
                null
        );

        assertTrue(profile.student());
        assertNull(profile.university());
    }

    @Test
    void universityWithoutDomainRequiresManualStudentReview() {
        Universidad university = university(2);
        when(universidadRepository.findByIdUniversidadAndEstadoTrue(2))
                .thenReturn(Optional.of(university));
        when(carreraRepository.findByIdCarreraAndIdUniversidadAndEstadoTrue(5, 2))
                .thenReturn(Optional.of(mock(Carrera.class)));
        when(dominioRepository.findByDominioCorreoIgnoreCaseAndEstadoTrue("gmail.com"))
                .thenReturn(Optional.empty());

        RegistrationProfile profile = service("").resolve(
                "student@gmail.com",
                2,
                5
        );

        assertTrue(profile.studentRequested());
        assertFalse(profile.automaticStudentAccess());
    }

    private RegistrationPolicyService service(String evaluatorEmails) {
        return new RegistrationPolicyService(
                universidadRepository,
                carreraRepository,
                dominioRepository,
                evaluatorEmails
        );
    }

    private Universidad university(Integer id) {
        Universidad university = mock(Universidad.class);
        org.mockito.Mockito.lenient()
                .when(university.getIdUniversidad())
                .thenReturn(id);
        return university;
    }

    private DominioUniversidad domain(Integer universityId, boolean automatic) {
        DominioUniversidad domain = mock(DominioUniversidad.class);
        when(domain.getIdUniversidad()).thenReturn(universityId);
        when(domain.isVerificacionAutomatica()).thenReturn(automatic);
        return domain;
    }
}
