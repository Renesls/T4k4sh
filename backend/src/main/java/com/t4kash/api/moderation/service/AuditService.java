package com.t4kash.api.moderation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.t4kash.api.moderation.entity.AuditoriaSistema;
import com.t4kash.api.moderation.repository.AuditoriaSistemaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditService {
    private final AuditoriaSistemaRepository auditRepository;
    private final ObjectMapper objectMapper;

    public AuditService(
            AuditoriaSistemaRepository auditRepository,
            ObjectMapper objectMapper
    ) {
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    public void record(
            Integer userId,
            String action,
            String affectedTable,
            Integer affectedId,
            Object before,
            Object after,
            String ipAddress,
            String userAgent
    ) {
        AuditoriaSistema audit = new AuditoriaSistema();
        audit.setIdUsuario(userId);
        audit.setAccion(action);
        audit.setTablaAfectada(affectedTable);
        audit.setIdRegistroAfectado(affectedId);
        audit.setDatosAntes(toJson(before));
        audit.setDatosDespues(toJson(after));
        audit.setIpOrigen(limit(ipAddress, 45));
        audit.setUserAgent(limit(userAgent, 500));
        audit.setFechaAccion(LocalDateTime.now());
        auditRepository.save(audit);
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(
                    "No se pudo registrar la auditoria de la operacion.",
                    exception
            );
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
