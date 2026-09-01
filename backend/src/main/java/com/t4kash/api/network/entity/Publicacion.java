package com.t4kash.api.network.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "publicaciones")
public class Publicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_publicacion")
    private Integer idPublicacion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_publicacion_origen")
    private Integer idPublicacionOrigen;

    @Column(name = "contenido", columnDefinition = "text")
    private String contenido;

    @Column(name = "tipo_publicacion", nullable = false, length = 40)
    private String tipoPublicacion;

    @Column(name = "visibilidad", nullable = false, length = 30)
    private String visibilidad;

    @Column(name = "permite_comentarios", nullable = false)
    private boolean permiteComentarios;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;

    @Column(name = "estado_publicacion", nullable = false, length = 30)
    private String estadoPublicacion;

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

    public Integer getIdPublicacionOrigen() {
        return idPublicacionOrigen;
    }

    public void setIdPublicacionOrigen(Integer idPublicacionOrigen) {
        this.idPublicacionOrigen = idPublicacionOrigen;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public String getTipoPublicacion() {
        return tipoPublicacion;
    }

    public void setTipoPublicacion(String tipoPublicacion) {
        this.tipoPublicacion = tipoPublicacion;
    }

    public String getVisibilidad() {
        return visibilidad;
    }

    public void setVisibilidad(String visibilidad) {
        this.visibilidad = visibilidad;
    }

    public boolean isPermiteComentarios() {
        return permiteComentarios;
    }

    public void setPermiteComentarios(boolean permiteComentarios) {
        this.permiteComentarios = permiteComentarios;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public LocalDateTime getFechaEdicion() {
        return fechaEdicion;
    }

    public void setFechaEdicion(LocalDateTime fechaEdicion) {
        this.fechaEdicion = fechaEdicion;
    }

    public String getEstadoPublicacion() {
        return estadoPublicacion;
    }

    public void setEstadoPublicacion(String estadoPublicacion) {
        this.estadoPublicacion = estadoPublicacion;
    }
}
