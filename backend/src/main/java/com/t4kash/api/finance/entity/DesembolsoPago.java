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
@Table(name = "desembolsos_pago")
public class DesembolsoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_desembolso")
    private Integer idDesembolso;
    @Column(name = "id_pago", nullable = false)
    private Integer idPago;
    @Column(name = "id_estudiante", nullable = false)
    private Integer idEstudiante;
    @Column(name = "id_metodo_cobro")
    private Integer idMetodoCobro;
    @Column(name = "monto_desembolso", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDesembolso;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda", nullable = false, length = 3, columnDefinition = "char(3)")
    private String moneda;
    @Column(name = "proveedor_desembolso", nullable = false, length = 30)
    private String proveedorDesembolso;
    @Column(name = "estado_desembolso", nullable = false, length = 30)
    private String estadoDesembolso;
    @Column(name = "clave_idempotencia", nullable = false, length = 150)
    private String claveIdempotencia;
    @Column(name = "referencia_destino", length = 100)
    private String referenciaDestino;
    @Column(name = "referencia_proveedor", length = 150)
    private String referenciaProveedor;
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;
    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;
    @Column(name = "ultimo_error", length = 700)
    private String ultimoError;

    public Integer getIdDesembolso() { return idDesembolso; }
    public Integer getIdPago() { return idPago; }
    public Integer getIdEstudiante() { return idEstudiante; }
    public BigDecimal getMontoDesembolso() { return montoDesembolso; }
    public String getMoneda() { return moneda; }
    public String getProveedorDesembolso() { return proveedorDesembolso; }
    public String getEstadoDesembolso() { return estadoDesembolso; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setIdPago(Integer value) { this.idPago = value; }
    public void setIdEstudiante(Integer value) { this.idEstudiante = value; }
    public void setIdMetodoCobro(Integer value) { this.idMetodoCobro = value; }
    public void setMontoDesembolso(BigDecimal value) { this.montoDesembolso = value; }
    public void setMoneda(String value) { this.moneda = value; }
    public void setProveedorDesembolso(String value) { this.proveedorDesembolso = value; }
    public void setEstadoDesembolso(String value) { this.estadoDesembolso = value; }
    public void setClaveIdempotencia(String value) { this.claveIdempotencia = value; }
    public void setReferenciaDestino(String value) { this.referenciaDestino = value; }
    public void setReferenciaProveedor(String value) { this.referenciaProveedor = value; }
    public void setFechaCreacion(LocalDateTime value) { this.fechaCreacion = value; }
    public void setFechaProcesamiento(LocalDateTime value) { this.fechaProcesamiento = value; }
    public void setFechaConfirmacion(LocalDateTime value) { this.fechaConfirmacion = value; }
    public void setUltimoError(String value) { this.ultimoError = value; }
}
