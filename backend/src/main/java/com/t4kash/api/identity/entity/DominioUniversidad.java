package com.t4kash.api.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "dominios_universidad")
public class DominioUniversidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dominio")
    private Integer idDominio;

    @Column(name = "id_universidad", nullable = false)
    private Integer idUniversidad;

    @Column(name = "dominio_correo", nullable = false, length = 100)
    private String dominioCorreo;

    @Column(name = "tipo_dominio", nullable = false, length = 30)
    private String tipoDominio;

    @Column(name = "verificacion_automatica", nullable = false)
    private boolean verificacionAutomatica;

    @Column(name = "estado", nullable = false)
    private boolean estado;

    public Integer getIdDominio() {
        return idDominio;
    }

    public Integer getIdUniversidad() {
        return idUniversidad;
    }

    public String getDominioCorreo() {
        return dominioCorreo;
    }

    public String getTipoDominio() {
        return tipoDominio;
    }

    public boolean isVerificacionAutomatica() {
        return verificacionAutomatica;
    }

    public boolean isEstado() {
        return estado;
    }
}
