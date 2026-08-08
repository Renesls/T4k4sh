package com.t4kash.api.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "carreras")
public class Carrera {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_carrera")
    private Integer idCarrera;

    @Column(name = "nombre_carrera", nullable = false, length = 120)
    private String nombreCarrera;

    @Column(name = "id_universidad", nullable = false)
    private Integer idUniversidad;

    @Column(name = "estado", nullable = false)
    private boolean estado;

    public Integer getIdCarrera() {
        return idCarrera;
    }

    public String getNombreCarrera() {
        return nombreCarrera;
    }

    public Integer getIdUniversidad() {
        return idUniversidad;
    }

    public boolean isEstado() {
        return estado;
    }
}
