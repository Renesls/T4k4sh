package com.t4kash.api.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "universidades")
public class Universidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_universidad")
    private Integer idUniversidad;

    @Column(name = "nombre_universidad", nullable = false, length = 150)
    private String nombreUniversidad;

    @Column(name = "dominio_correo", length = 100)
    private String dominioCorreo;

    @Column(name = "estado", nullable = false)
    private boolean estado;

    public Integer getIdUniversidad() {
        return idUniversidad;
    }

    public String getNombreUniversidad() {
        return nombreUniversidad;
    }

    public String getDominioCorreo() {
        return dominioCorreo;
    }

    public boolean isEstado() {
        return estado;
    }
}
