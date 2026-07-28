package com.t4kash.api.identity.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.t4kash.api.identity.entity.Carrera;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
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

    @Test
    void normalEmailCreatesClientProfile() {
        Universidad university = university(1, "uamv.edu.ni");
        when(universidadRepository.findAllByEstadoTrueOrderByNombreUniversidad())
                .thenReturn(List.of(university));

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
        Universidad university = university(1, "uamv.edu.ni");
        when(universidadRepository.findAllByEstadoTrueOrderByNombreUniversidad())
                .thenReturn(List.of(university));

        assertThrows(
                IllegalArgumentException.class,
                () -> service("").resolve("student@uamv.edu.ni", null, null)
        );
    }

    @Test
    void verifiedUniversitySelectionCreatesStudentProfile() {
        Universidad university = university(1, "uamv.edu.ni");
        when(universidadRepository.findByIdUniversidadAndEstadoTrue(1))
                .thenReturn(Optional.of(university));
        when(carreraRepository.findByIdCarreraAndIdUniversidad(4, 1))
                .thenReturn(Optional.of(mock(Carrera.class)));

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

    private RegistrationPolicyService service(String evaluatorEmails) {
        return new RegistrationPolicyService(
                universidadRepository,
                carreraRepository,
                evaluatorEmails
        );
    }

    private Universidad university(Integer id, String domain) {
        Universidad university = mock(Universidad.class);
        org.mockito.Mockito.lenient()
                .when(university.getIdUniversidad())
                .thenReturn(id);
        org.mockito.Mockito.lenient()
                .when(university.getDominioCorreo())
                .thenReturn(domain);
        return university;
    }
}
