package com.t4kash.api.identity.service;

import com.t4kash.api.exception.ResourceNotFoundException;
import com.t4kash.api.identity.dto.CareerResponse;
import com.t4kash.api.identity.dto.UniversityResponse;
import com.t4kash.api.identity.entity.Carrera;
import com.t4kash.api.identity.entity.DominioUniversidad;
import com.t4kash.api.identity.entity.Universidad;
import com.t4kash.api.identity.repository.CarreraRepository;
import com.t4kash.api.identity.repository.DominioUniversidadRepository;
import com.t4kash.api.identity.repository.UniversidadRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class IdentityCatalogService {
    private final UniversidadRepository universidadRepository;
    private final CarreraRepository carreraRepository;
    private final DominioUniversidadRepository dominioRepository;

    public IdentityCatalogService(
            UniversidadRepository universidadRepository,
            CarreraRepository carreraRepository,
            DominioUniversidadRepository dominioRepository
    ) {
        this.universidadRepository = universidadRepository;
        this.carreraRepository = carreraRepository;
        this.dominioRepository = dominioRepository;
    }

    public List<UniversityResponse> getUniversities() {
        List<Universidad> universities = universidadRepository
                .findAllByEstadoTrueOrderByNombreUniversidad();
        Map<Integer, List<String>> domainsByUniversity = dominioRepository
                .findAllByIdUniversidadInAndEstadoTrue(
                        universities.stream()
                                .map(Universidad::getIdUniversidad)
                                .toList()
                )
                .stream()
                .collect(Collectors.groupingBy(
                        DominioUniversidad::getIdUniversidad,
                        Collectors.mapping(
                                DominioUniversidad::getDominioCorreo,
                                Collectors.toList()
                        )
                ));
        return universities.stream()
                .map(university -> toResponse(
                        university,
                        domainsByUniversity.getOrDefault(
                                university.getIdUniversidad(),
                                List.of()
                        )
                ))
                .toList();
    }

    public List<CareerResponse> getCareers(Integer universityId) {
        universidadRepository.findByIdUniversidadAndEstadoTrue(universityId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "La universidad seleccionada no existe o esta inactiva."
                ));
        return carreraRepository
                .findAllByIdUniversidadAndEstadoTrueOrderByNombreCarrera(universityId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private UniversityResponse toResponse(
            Universidad universidad,
            List<String> domains
    ) {
        return new UniversityResponse(
                universidad.getIdUniversidad(),
                universidad.getNombreUniversidad(),
                domains.stream().sorted().toList()
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
