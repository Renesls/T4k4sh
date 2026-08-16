package com.t4kash.api.network.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "reacciones_publicacion")
public class ReaccionPublicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reaccion")
    private Integer idReaccion;

    @Column(name = "id_publicacion", nullable = false)
    private Integer idPublicacion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "tipo_reaccion", nullable = false, length = 30)
    private String tipoReaccion;

    @Column(name = "fecha_reaccion", nullable = false)
    private LocalDateTime fechaReaccion;

    public Integer getIdReaccion() {
        return idReaccion;
    }

    public void setIdReaccion(Integer idReaccion) {
        this.idReaccion = idReaccion;
    }

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

    public String getTipoReaccion() {
        return tipoReaccion;
    }

    public void setTipoReaccion(String tipoReaccion) {
        this.tipoReaccion = tipoReaccion;
    }

    public LocalDateTime getFechaReaccion() {
        return fechaReaccion;
    }

    public void setFechaReaccion(LocalDateTime fechaReaccion) {
        this.fechaReaccion = fechaReaccion;
    }
}
