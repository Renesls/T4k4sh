package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.CareerResponse;
import com.t4kash.api.identity.dto.UniversityResponse;
import com.t4kash.api.identity.entity.Carrera;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class IdentityCatalogService {
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;

    public IdentityCatalogService(
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository
    ) {
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
    }

    public List<UniversityResponse> getUniversities() {
        return universidadRepository.findAllByEstadoTrueOrderByNombreUniversidad()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public List<CareerResponse> getCareers(Integer universityId) {
        universidadRepository.findByIdUniversidadAndEstadoTrue(universityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La universidad seleccionada no existe o esta inactiva."
                ));
        return carreraRepository.findAllByIdUniversidadOrderByNombreCarrera(universityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UniversityResponse toResponse(Universidad universidad) {
        return new UniversityResponse(
                universidad.getIdUniversidad(),
                universidad.getNombreUniversidad(),
                universidad.getDominioCorreo()
        );
    }

    private CareerResponse toResponse(Carrera carrera) {
        return new CareerResponse(
                carrera.getIdCarrera(),
                carrera.getNombreCarrera(),
                carrera.getIdUniversidad()
        );
    }
}
