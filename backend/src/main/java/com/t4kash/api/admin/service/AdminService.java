package com.t4kash.api.admin.service;

import com.t4kash.api.admin.dto.AdminSummaryResponse;
import com.t4kash.api.identity.service.StudentVerificationService;
import com.t4kash.api.marketplace.dto.TaskResponse;
import com.t4kash.api.marketplace.repository.TareaRepository;
import com.t4kash.api.marketplace.repository.TrabajoAsignadoRepository;
import com.t4kash.api.marketplace.repository.UsuarioRepository;
import com.t4kash.api.marketplace.service.MarketplaceService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AdminService {
    private final UsuarioRepository usuarioRepository;
    private final TareaRepository tareaRepository;
    private final TrabajoAsignadoRepository trabajoRepository;
    private final StudentVerificationService verificationService;
    private final MarketplaceService marketplaceService;

    public AdminService(
            UsuarioRepository usuarioRepository,
            TareaRepository tareaRepository,
            TrabajoAsignadoRepository trabajoRepository,
            StudentVerificationService verificationService,
            MarketplaceService marketplaceService
    ) {
        this.usuarioRepository = usuarioRepository;
        this.tareaRepository = tareaRepository;
        this.trabajoRepository = trabajoRepository;
        this.verificationService = verificationService;
        this.marketplaceService = marketplaceService;
    }

    @Transactional(readOnly = true)
    public AdminSummaryResponse getSummary() {
        return new AdminSummaryResponse(
                usuarioRepository.count(),
                verificationService.countPending(),
                tareaRepository.countByEstadoTareaIgnoreCase("PUBLICADA"),
                trabajoRepository.count()
        );
    }

    @Transactional
    public List<TaskResponse> listTasks() {
        return marketplaceService.listTasksForAdmin();
    }

    @Transactional
    public TaskResponse cancelTask(Integer taskId) {
        return marketplaceService.cancelTaskAsAdmin(taskId);
    }
}
