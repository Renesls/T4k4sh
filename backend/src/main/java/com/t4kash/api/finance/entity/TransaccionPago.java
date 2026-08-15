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
import java.util.Map;

@Entity
@Table(name = "transacciones_pago")
public class TransaccionPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transaccion")
    private Long idTransaccion;
    @Column(name = "id_pago", nullable = false)
    private Integer idPago;
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @Column(name = "tipo_movimiento", nullable = false, length = 40)
    private String tipoMovimiento;
    @Column(name = "saldo_afectado", nullable = false, length = 30)
    private String saldoAfectado;
    @Column(name = "monto", nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "moneda", nullable = false, length = 3, columnDefinition = "char(3)")
    private String moneda;
    @Column(name = "estado_movimiento", nullable = false, length = 30)
    private String estadoMovimiento;
    @Column(name = "proveedor_pago", nullable = false, length = 30)
    private String proveedorPago;
    @Column(name = "clave_idempotencia", nullable = false, length = 150)
    private String claveIdempotencia;
    @Column(name = "referencia_proveedor", length = 150)
    private String referenciaProveedor;
    @Column(name = "fecha_evento_proveedor")
    private LocalDateTime fechaEventoProveedor;
    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;
    @Column(name = "descripcion", length = 300)
    private String descripcion;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadatos", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadatos;

    public Long getIdTransaccion() { return idTransaccion; }
    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer value) { this.idPago = value; }
    public Integer getIdUsuario() { return idUsuario; }
    public void setIdUsuario(Integer value) { this.idUsuario = value; }
    public String getTipoMovimiento() { return tipoMovimiento; }
    public void setTipoMovimiento(String value) { this.tipoMovimiento = value; }
    public String getSaldoAfectado() { return saldoAfectado; }
    public void setSaldoAfectado(String value) { this.saldoAfectado = value; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal value) { this.monto = value; }
    public String getMoneda() { return moneda; }
    public void setMoneda(String value) { this.moneda = value; }
    public String getEstadoMovimiento() { return estadoMovimiento; }
    public void setEstadoMovimiento(String value) { this.estadoMovimiento = value; }
    public String getProveedorPago() { return proveedorPago; }
    public void setProveedorPago(String value) { this.proveedorPago = value; }
    public String getClaveIdempotencia() { return claveIdempotencia; }
    public void setClaveIdempotencia(String value) { this.claveIdempotencia = value; }
    public String getReferenciaProveedor() { return referenciaProveedor; }
    public void setReferenciaProveedor(String value) { this.referenciaProveedor = value; }
    public LocalDateTime getFechaEventoProveedor() { return fechaEventoProveedor; }
    public void setFechaEventoProveedor(LocalDateTime value) { this.fechaEventoProveedor = value; }
    public LocalDateTime getFechaRegistro() { return fechaRegistro; }
    public void setFechaRegistro(LocalDateTime value) { this.fechaRegistro = value; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String value) { this.descripcion = value; }
    public Map<String, Object> getMetadatos() { return metadatos; }
    public void setMetadatos(Map<String, Object> value) { this.metadatos = value; }
}
