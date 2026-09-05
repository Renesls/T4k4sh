package com.t4kash.api.network.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios_publicacion")
public class ComentarioPublicacion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario_publicacion")
    private Integer idComentarioPublicacion;

    @Column(name = "id_publicacion", nullable = false)
    private Integer idPublicacion;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "id_comentario_padre")
    private Integer idComentarioPadre;

    @Column(name = "contenido", nullable = false, columnDefinition = "text")
    private String contenido;

    @Column(name = "fecha_comentario", nullable = false)
    private LocalDateTime fechaComentario;

    @Column(name = "fecha_edicion")
    private LocalDateTime fechaEdicion;

    @Column(name = "estado_comentario", nullable = false, length = 30)
    private String estadoComentario;

    public Integer getIdComentarioPublicacion() {
        return idComentarioPublicacion;
    }

    public void setIdComentarioPublicacion(Integer idComentarioPublicacion) {
        this.idComentarioPublicacion = idComentarioPublicacion;
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

    public Integer getIdComentarioPadre() {
        return idComentarioPadre;
    }

    public void setIdComentarioPadre(Integer idComentarioPadre) {
        this.idComentarioPadre = idComentarioPadre;
    }

    public String getContenido() {
        return contenido;
    }

    public void setContenido(String contenido) {
        this.contenido = contenido;
    }

    public LocalDateTime getFechaComentario() {
        return fechaComentario;
    }

    public void setFechaComentario(LocalDateTime fechaComentario) {
        this.fechaComentario = fechaComentario;
    }

    public LocalDateTime getFechaEdicion() {
        return fechaEdicion;
    }

    public void setFechaEdicion(LocalDateTime fechaEdicion) {
        this.fechaEdicion = fechaEdicion;
    }

    public String getEstadoComentario() {
        return estadoComentario;
    }

    public void setEstadoComentario(String estadoComentario) {
        this.estadoComentario = estadoComentario;
    }
}
