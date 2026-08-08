package com.t4kash.api.identity.service;

import com.t4kash.api.marketplace.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UsernameServiceTest {
    private final UsuarioRepository repository = mock(UsuarioRepository.class);
    private final UsernameService service = new UsernameService(repository);

    @Test
    void generatesNormalizedUsername() {
        assertEquals("rene.sandoval", service.generate("René", "Sandoval"));
    }

    @Test
    void appendsSuffixWhenUsernameAlreadyExists() {
        when(repository.existsByNombreUsuarioIgnoreCase("rene.sandoval"))
                .thenReturn(true);
        when(repository.existsByNombreUsuarioIgnoreCase("rene.sandoval2"))
                .thenReturn(true);

        assertEquals("rene.sandoval3", service.generate("Rene", "Sandoval"));
    }
}
