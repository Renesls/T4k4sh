package com.t4kash.api.identity.service;

import com.t4kash.api.identity.entity.DominioUniversidad;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.DominioUniversidadRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IdentityCatalogServiceTest {
    @Mock private UniversidadRepository universidadRepository;
    @Mock private CarreraRepository carreraRepository;
    @Mock private DominioUniversidadRepository dominioRepository;

    private IdentityCatalogService service;

    @BeforeEach
    void setUp() {
        service = new IdentityCatalogService(
                universidadRepository,
                carreraRepository,
                dominioRepository
        );
    }

    @Test
    void includesActiveDomainsInUniversityCatalog() {
        Universidad university = mock(Universidad.class);
        when(university.getIdUniversidad()).thenReturn(4);
        when(university.getNombreUniversidad()).thenReturn("Universidad Americana");
        DominioUniversidad domain = mock(DominioUniversidad.class);
        when(domain.getIdUniversidad()).thenReturn(4);
        when(domain.getDominioCorreo()).thenReturn("uamv.edu.ni");
        when(universidadRepository.findAllByEstadoTrueOrderByNombreUniversidad())
                .thenReturn(List.of(university));
        when(dominioRepository.findAllByIdUniversidadInAndEstadoTrue(any()))
                .thenReturn(List.of(domain));

        var response = service.getUniversities();

        assertEquals(1, response.size());
        assertEquals(List.of("uamv.edu.ni"), response.getFirst().dominiosCorreo());
    }
}
