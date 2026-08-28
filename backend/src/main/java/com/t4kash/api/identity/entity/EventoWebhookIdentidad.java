package com.t4kash.api.identity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_webhook_identidad")
public class EventoWebhookIdentidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_webhook_identidad")
    private Integer idEventoWebhookIdentidad;

    @Column(name = "id_verificacion_identidad", nullable = false)
    private Integer idVerificacionIdentidad;

    @Column(name = "id_evento_proveedor", length = 150)
    private String idEventoProveedor;

    @Column(name = "clave_idempotencia", nullable = false, length = 64)
    private String claveIdempotencia;

    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;

    @Column(name = "estado_reportado", length = 40)
    private String estadoReportado;

    @Column(name = "firma_valida", nullable = false)
    private boolean firmaValida;

    @Column(name = "estado_procesamiento", nullable = false, length = 30)
    private String estadoProcesamiento;

    @Column(name = "hash_contenido", nullable = false, length = 64)
    private String hashContenido;

    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;

    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "detalle_error", length = 500)
    private String detalleError;

    public Integer getIdEventoWebhookIdentidad() {
        return idEventoWebhookIdentidad;
    }

    public void setIdEventoWebhookIdentidad(Integer idEventoWebhookIdentidad) {
        this.idEventoWebhookIdentidad = idEventoWebhookIdentidad;
    }

    public Integer getIdVerificacionIdentidad() {
        return idVerificacionIdentidad;
    }

    public void setIdVerificacionIdentidad(Integer idVerificacionIdentidad) {
        this.idVerificacionIdentidad = idVerificacionIdentidad;
    }

    public String getIdEventoProveedor() {
        return idEventoProveedor;
    }

    public void setIdEventoProveedor(String idEventoProveedor) {
        this.idEventoProveedor = idEventoProveedor;
    }

    public String getClaveIdempotencia() {
        return claveIdempotencia;
    }

    public void setClaveIdempotencia(String claveIdempotencia) {
        this.claveIdempotencia = claveIdempotencia;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getEstadoReportado() {
        return estadoReportado;
    }

    public void setEstadoReportado(String estadoReportado) {
        this.estadoReportado = estadoReportado;
    }

    public boolean isFirmaValida() {
        return firmaValida;
    }

    public void setFirmaValida(boolean firmaValida) {
        this.firmaValida = firmaValida;
    }

    public String getEstadoProcesamiento() {
        return estadoProcesamiento;
    }

    public void setEstadoProcesamiento(String estadoProcesamiento) {
        this.estadoProcesamiento = estadoProcesamiento;
    }

    public String getHashContenido() {
        return hashContenido;
    }

    public void setHashContenido(String hashContenido) {
        this.hashContenido = hashContenido;
    }

    public LocalDateTime getFechaRecepcion() {
        return fechaRecepcion;
    }

    public void setFechaRecepcion(LocalDateTime fechaRecepcion) {
        this.fechaRecepcion = fechaRecepcion;
    }

    public LocalDateTime getFechaProcesamiento() {
        return fechaProcesamiento;
    }

    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) {
        this.fechaProcesamiento = fechaProcesamiento;
    }

    public String getDetalleError() {
        return detalleError;
    }

    public void setDetalleError(String detalleError) {
        this.detalleError = detalleError;
    }
}
