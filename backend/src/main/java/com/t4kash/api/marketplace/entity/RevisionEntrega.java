package com.t4kash.api.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "revisiones_entrega")
public class RevisionEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_revision_entrega")
    private Integer idRevisionEntrega;

    @Column(name = "id_entrega", nullable = false)
    private Integer idEntrega;

    @Column(name = "id_usuario_revisa", nullable = false)
    private Integer idUsuarioRevisa;

    @Column(name = "resultado_revision", nullable = false, length = 40)
    private String resultadoRevision;

    @Column(name = "observacion", length = 700)
    private String observacion;

    @Column(name = "fecha_revision", nullable = false)
    private LocalDateTime fechaRevision;

    @Column(name = "estado_revision", nullable = false, length = 30)
    private String estadoRevision;

    public Integer getIdRevisionEntrega() {
        return idRevisionEntrega;
    }

    public void setIdRevisionEntrega(Integer idRevisionEntrega) {
        this.idRevisionEntrega = idRevisionEntrega;
    }

    public Integer getIdEntrega() {
        return idEntrega;
    }

    public void setIdEntrega(Integer idEntrega) {
        this.idEntrega = idEntrega;
    }

    public Integer getIdUsuarioRevisa() {
        return idUsuarioRevisa;
    }

    public void setIdUsuarioRevisa(Integer idUsuarioRevisa) {
        this.idUsuarioRevisa = idUsuarioRevisa;
    }

    public String getResultadoRevision() {
        return resultadoRevision;
    }

    public void setResultadoRevision(String resultadoRevision) {
        this.resultadoRevision = resultadoRevision;
    }

    public String getObservacion() {
        return observacion;
    }

    public void setObservacion(String observacion) {
        this.observacion = observacion;
    }

    public LocalDateTime getFechaRevision() {
        return fechaRevision;
    }

    public void setFechaRevision(LocalDateTime fechaRevision) {
        this.fechaRevision = fechaRevision;
    }

    public String getEstadoRevision() {
        return estadoRevision;
    }

    public void setEstadoRevision(String estadoRevision) {
        this.estadoRevision = estadoRevision;
    }
}
