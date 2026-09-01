package com.t4kash.api.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "reembolsos_pago")
public class ReembolsoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reembolso")
    private Integer idReembolso;
    @Column(name = "id_pago", nullable = false)
    private Integer idPago;
    @Column(name = "id_disputa")
    private Integer idDisputa;
    @Column(name = "id_usuario_solicita")
    private Integer idUsuarioSolicita;
    @Column(name = "monto_reembolso", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoReembolso;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda", nullable = false, length = 3, columnDefinition = "char(3)")
    private String moneda;
    @Column(name = "motivo", nullable = false, length = 500)
    private String motivo;
    @Column(name = "estado_reembolso", nullable = false, length = 30)
    private String estadoReembolso;
    @Column(name = "clave_idempotencia", nullable = false, length = 150)
    private String claveIdempotencia;
    @Column(name = "referencia_proveedor", length = 150)
    private String referenciaProveedor;
    @Column(name = "fecha_solicitud", nullable = false)
    private LocalDateTime fechaSolicitud;
    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;
    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;
    @Column(name = "ultimo_error", length = 700)
    private String ultimoError;

    public Integer getIdReembolso() { return idReembolso; }
    public Integer getIdPago() { return idPago; }
    public Integer getIdDisputa() { return idDisputa; }
    public BigDecimal getMontoReembolso() { return montoReembolso; }
    public String getMoneda() { return moneda; }
    public String getMotivo() { return motivo; }
    public String getEstadoReembolso() { return estadoReembolso; }
    public LocalDateTime getFechaSolicitud() { return fechaSolicitud; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setIdPago(Integer value) { this.idPago = value; }
    public void setIdDisputa(Integer value) { this.idDisputa = value; }
    public void setIdUsuarioSolicita(Integer value) { this.idUsuarioSolicita = value; }
    public void setMontoReembolso(BigDecimal value) { this.montoReembolso = value; }
    public void setMoneda(String value) { this.moneda = value; }
    public void setMotivo(String value) { this.motivo = value; }
    public void setEstadoReembolso(String value) { this.estadoReembolso = value; }
    public void setClaveIdempotencia(String value) { this.claveIdempotencia = value; }
    public void setReferenciaProveedor(String value) { this.referenciaProveedor = value; }
    public void setFechaSolicitud(LocalDateTime value) { this.fechaSolicitud = value; }
    public void setFechaProcesamiento(LocalDateTime value) { this.fechaProcesamiento = value; }
    public void setFechaConfirmacion(LocalDateTime value) { this.fechaConfirmacion = value; }
    public void setUltimoError(String value) { this.ultimoError = value; }
}
