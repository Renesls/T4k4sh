package com.t4kash.api.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "comentarios_entrega")
public class ComentarioEntrega {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_comentario_entrega")
    private Integer idComentarioEntrega;

    @Column(name = "id_entrega", nullable = false)
    private Integer idEntrega;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "comentario", nullable = false, length = 700)
    private String comentario;

    @Column(name = "tipo_comentario", nullable = false, length = 40)
    private String tipoComentario;

    @Column(name = "fecha_comentario", nullable = false)
    private LocalDateTime fechaComentario;

    public Integer getIdComentarioEntrega() {
        return idComentarioEntrega;
    }

    public void setIdComentarioEntrega(Integer idComentarioEntrega) {
        this.idComentarioEntrega = idComentarioEntrega;
    }

    public Integer getIdEntrega() {
        return idEntrega;
    }

    public void setIdEntrega(Integer idEntrega) {
        this.idEntrega = idEntrega;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public String getTipoComentario() {
        return tipoComentario;
    }

    public void setTipoComentario(String tipoComentario) {
        this.tipoComentario = tipoComentario;
    }

    public LocalDateTime getFechaComentario() {
        return fechaComentario;
    }

    public void setFechaComentario(LocalDateTime fechaComentario) {
        this.fechaComentario = fechaComentario;
    }
}
