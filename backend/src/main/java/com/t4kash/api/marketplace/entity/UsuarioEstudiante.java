package com.t4kash.api.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios_estudiantes")
public class UsuarioEstudiante {
    @Id
    @Column(name = "id_usuario")
    private Integer idUsuario;

    @Column(name = "id_carrera")
    private Integer idCarrera;

    @Column(name = "carnet_universitario", length = 50)
    private String carnetUniversitario;

    @Column(name = "estado_perfil_estudiante", nullable = false, length = 30)
    private String estadoPerfilEstudiante;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public Integer getIdCarrera() {
        return idCarrera;
    }

    public void setIdCarrera(Integer idCarrera) {
        this.idCarrera = idCarrera;
    }

    public String getCarnetUniversitario() {
        return carnetUniversitario;
    }

    public void setCarnetUniversitario(String carnetUniversitario) {
        this.carnetUniversitario = carnetUniversitario;
    }

    public String getEstadoPerfilEstudiante() {
        return estadoPerfilEstudiante;
    }

    public void setEstadoPerfilEstudiante(String estadoPerfilEstudiante) {
        this.estadoPerfilEstudiante = estadoPerfilEstudiante;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }
}
