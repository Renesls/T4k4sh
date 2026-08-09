package com.t4kash.api.identity.controller;

import com.t4kash.api.identity.dto.AuthenticatedUserResponse;
import com.t4kash.api.identity.dto.PublicProfileResponse;
import com.t4kash.api.identity.dto.UpdateUsernameRequest;
import com.t4kash.api.identity.service.PublicProfileService;
import com.t4kash.api.identity.web.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/profiles")
@Tag(name = "Perfiles", description = "Identidad publica y nombre de usuario")
public class PublicProfileController {
    private final PublicProfileService profileService;

    public PublicProfileController(PublicProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping("/{username}")
    @Operation(summary = "Consultar un perfil publico por nombre de usuario")
    public PublicProfileResponse getProfile(@PathVariable String username) {
        return profileService.getProfile(username);
    }

    @PutMapping("/me/username")
    @Operation(summary = "Cambiar mi nombre de usuario")
    @SecurityRequirement(name = "bearerAuth")
    public PublicProfileResponse updateUsername(
            @CurrentUser AuthenticatedUserResponse user,
            @Valid @RequestBody UpdateUsernameRequest request
    ) {
        return profileService.updateUsername(
                user.idUsuario(),
                request.nombreUsuario()
        );
    }
}
