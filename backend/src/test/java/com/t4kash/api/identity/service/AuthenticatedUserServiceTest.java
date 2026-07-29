package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ForbiddenOperationException;
import com.t4kash.api.exception.InvalidCredentialsException;
import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthenticatedUserServiceTest {
    @Mock
    private AuthService authService;

    @Test
    void resolvesBearerTokenAndReturnsAuthenticatedUser() {
        AuthenticatedUserResponse user = userWithRoles("CLIENTE");
        when(authService.getCurrentUser("token-seguro")).thenReturn(user);
        AuthenticatedUserService service = new AuthenticatedUserService(authService);

        AuthenticatedUserResponse response =
                service.requireUser("Bearer token-seguro");

        assertEquals(12, response.idUsuario());
        verify(authService).getCurrentUser("token-seguro");
    }

    @Test
    void rejectsRequestsWithoutBearerToken() {
        AuthenticatedUserService service = new AuthenticatedUserService(authService);

        assertThrows(
                InvalidCredentialsException.class,
                () -> service.requireUser(null)
        );
    }

    @Test
    void rejectsUsersWithoutRequiredRole() {
        when(authService.getCurrentUser("token-seguro"))
                .thenReturn(userWithRoles("ESTUDIANTE"));
        AuthenticatedUserService service = new AuthenticatedUserService(authService);

        assertThrows(
                ForbiddenOperationException.class,
                () -> service.requireRole("Bearer token-seguro", "CLIENTE")
        );
    }

    private AuthenticatedUserResponse userWithRoles(String... roles) {
        return new AuthenticatedUserResponse(
                12,
                "Rene",
                "Sandoval",
                "estudiante@universidad.edu",
                1,
                "Universidad Demo",
                2,
                "Diseno Grafico",
                "ACTIVO",
                List.of(roles)
        );
    }
}
