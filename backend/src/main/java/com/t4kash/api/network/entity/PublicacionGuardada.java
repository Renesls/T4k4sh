package com.t4kash.api.network.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@IdClass(PublicacionGuardadaId.class)
@Table(name = "publicaciones_guardadas")
public class PublicacionGuardada {
    @Id
    @Column(name = "id_publicacion")
    private Integer idPublicacion;

    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "fecha_guardado", nullable = false)
    private LocalDateTime fechaGuardado;

    public Integer getIdPublicacion() {
        return idPublicacion;
    }

    public void setIdPublicacion(Integer idPublicacion) {
        this.idPublicacion = idPublicacion;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public LocalDateTime getFechaGuardado() {
        return fechaGuardado;
    }

    public void setFechaGuardado(LocalDateTime fechaGuardado) {
        this.fechaGuardado = fechaGuardado;
    }
}
