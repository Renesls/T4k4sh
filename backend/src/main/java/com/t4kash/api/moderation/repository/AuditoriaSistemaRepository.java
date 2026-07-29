package com.t4kash.api.moderation.repository;

import com.t4kash.api.moderation.entity.AuditoriaSistema;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditoriaSistemaRepository
        extends JpaRepository<AuditoriaSistema, Integer> {
}
