package com.t4kash.api.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "verificaciones_identidad")
public class VerificacionIdentidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_verificacion_identidad")
    private Integer idVerificacionIdentidad;

    @Column(name = "id_usuario", nullable = false)
    private Integer idUsuario;

    @Column(name = "proveedor", nullable = false, length = 30)
    private String proveedor;

    @Column(name = "id_sesion_proveedor", nullable = false)
    private UUID idSesionProveedor;

    @Column(name = "id_flujo_proveedor")
    private UUID idFlujoProveedor;

    @Column(name = "version_flujo")
    private Integer versionFlujo;

    @Column(name = "origen_solicitud", nullable = false, length = 30)
    private String origenSolicitud;

    @Column(name = "estado_verificacion", nullable = false, length = 30)
    private String estadoVerificacion;

    @Column(name = "estado_proveedor", nullable = false, length = 40)
    private String estadoProveedor;

    @Column(name = "huella_documento", length = 64)
    private String huellaDocumento;

    @Column(name = "motivo_resultado", length = 500)
    private String motivoResultado;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;

    public Integer getIdVerificacionIdentidad() {
        return idVerificacionIdentidad;
    }

    public void setIdVerificacionIdentidad(Integer idVerificacionIdentidad) {
        this.idVerificacionIdentidad = idVerificacionIdentidad;
    }

    public Integer getIdUsuario() {
        return idUsuario;
    }

    public void setIdUsuario(Integer idUsuario) {
        this.idUsuario = idUsuario;
    }

    public String getProveedor() {
        return proveedor;
    }

    public void setProveedor(String proveedor) {
        this.proveedor = proveedor;
    }

    public UUID getIdSesionProveedor() {
        return idSesionProveedor;
    }

    public void setIdSesionProveedor(UUID idSesionProveedor) {
        this.idSesionProveedor = idSesionProveedor;
    }

    public UUID getIdFlujoProveedor() {
        return idFlujoProveedor;
    }

    public void setIdFlujoProveedor(UUID idFlujoProveedor) {
        this.idFlujoProveedor = idFlujoProveedor;
    }

    public Integer getVersionFlujo() {
        return versionFlujo;
    }

    public void setVersionFlujo(Integer versionFlujo) {
        this.versionFlujo = versionFlujo;
    }

    public String getOrigenSolicitud() {
        return origenSolicitud;
    }

    public void setOrigenSolicitud(String origenSolicitud) {
        this.origenSolicitud = origenSolicitud;
    }

    public String getEstadoVerificacion() {
        return estadoVerificacion;
    }

    public void setEstadoVerificacion(String estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    public String getEstadoProveedor() {
        return estadoProveedor;
    }

    public void setEstadoProveedor(String estadoProveedor) {
        this.estadoProveedor = estadoProveedor;
    }

    public String getHuellaDocumento() {
        return huellaDocumento;
    }

    public void setHuellaDocumento(String huellaDocumento) {
        this.huellaDocumento = huellaDocumento;
    }

    public String getMotivoResultado() {
        return motivoResultado;
    }

    public void setMotivoResultado(String motivoResultado) {
        this.motivoResultado = motivoResultado;
    }

    public LocalDateTime getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDateTime fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }

    public LocalDateTime getFechaDecision() {
        return fechaDecision;
    }

    public void setFechaDecision(LocalDateTime fechaDecision) {
        this.fechaDecision = fechaDecision;
    }

    public LocalDateTime getFechaExpiracion() {
        return fechaExpiracion;
    }

    public void setFechaExpiracion(LocalDateTime fechaExpiracion) {
        this.fechaExpiracion = fechaExpiracion;
    }
}
