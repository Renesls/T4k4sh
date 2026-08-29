package com.t4kash.api.marketplace.entity;

import com.t4kash.api.identity.entity.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "evaluaciones")
public class Evaluacion {

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getEvaluador() {
        return evaluador;
    }

    public void setEvaluador(User evaluador) {
        this.evaluador = evaluador;
    }

    public User getEvaluado() {
        return evaluado;
    }

    public void setEvaluado(User evaluado) {
        this.evaluado = evaluado;
    }

    public TrabajoAsignado getTrabajoAsignado() {
        return trabajoAsignado;
    }

    public void setTrabajoAsignado(TrabajoAsignado trabajoAsignado) {
        this.trabajoAsignado = trabajoAsignado;
    }

    public Integer getCalificacion() {
        return calificacion;
    }

    public void setCalificacion(Integer calificacion) {
        this.calificacion = calificacion;
    }

    public String getComentario() {
        return comentario;
    }

    public void setComentario(String comentario) {
        this.comentario = comentario;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluador_id", nullable = false)
    private User evaluador;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluado_id", nullable = false)
    private User evaluado;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trabajo_asignado_id", nullable = false)
    private TrabajoAsignado trabajoAsignado;

    @Column(nullable = false)
    private Integer calificacion; // Del 1 al 5

    @Column(length = 500)
    private String comentario;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion = LocalDateTime.now();


}