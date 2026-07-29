package com.t4kash.api.identity.controller;

import com.t4kash.api.identity.dto.CareerResponse;
import com.t4kash.api.identity.dto.UniversityResponse;
import com.t4kash.api.identity.service.IdentityCatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/identity")
public class IdentityCatalogController {
    private final IdentityCatalogService catalogService;

    public IdentityCatalogController(IdentityCatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/universities")
    public List<UniversityResponse> universities() {
        return catalogService.getUniversities();
    }

    @GetMapping("/universities/{universityId}/careers")
    public List<CareerResponse> careers(
            @PathVariable Integer universityId
    ) {
        return catalogService.getCareers(universityId);
    }
}
