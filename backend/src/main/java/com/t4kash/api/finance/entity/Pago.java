package com.t4kash.api.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "pagos")
public class Pago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pago")
    private Integer idPago;
    @Column(name = "id_trabajo", nullable = false)
    private Integer idTrabajo;
    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;
    @Column(name = "id_estudiante", nullable = false)
    private Integer idEstudiante;
    @Column(name = "uuid_pago", nullable = false)
    private UUID uuidPago;
    @Column(name = "proveedor_pago", nullable = false, length = 30)
    private String proveedorPago;
    @Column(name = "entorno_pago", nullable = false, length = 20)
    private String entornoPago;
    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago;
    @Column(name = "moneda_cobro", nullable = false, length = 3, columnDefinition = "char(3)")
    private String monedaCobro;
    @Column(name = "moneda_procesamiento", length = 3, columnDefinition = "char(3)")
    private String monedaProcesamiento;
    @Column(name = "tipo_cambio", precision = 18, scale = 8)
    private BigDecimal tipoCambio;
    @Column(name = "monto_procesamiento", precision = 12, scale = 2)
    private BigDecimal montoProcesamiento;
    @Column(name = "monto_estudiante", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoEstudiante;
    @Column(name = "porcentaje_comision_plataforma", nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentajeComisionPlataforma;
    @Column(name = "comision_plataforma", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionPlataforma;
    @Column(name = "comision_procesador", nullable = false, precision = 12, scale = 2)
    private BigDecimal comisionProcesador;
    @Column(name = "impuesto_procesador", nullable = false, precision = 12, scale = 2)
    private BigDecimal impuestoProcesador;
    @Column(name = "monto_total_cliente", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoTotalCliente;
    @Column(name = "estado_pago", nullable = false, length = 40)
    private String estadoPago;
    @Column(name = "referencia_comercio", length = 100)
    private String referenciaComercio;
    @Column(name = "referencia_proveedor", length = 150)
    private String referenciaProveedor;
    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
    @Column(name = "fecha_expiracion")
    private LocalDateTime fechaExpiracion;
    @Column(name = "fecha_confirmacion")
    private LocalDateTime fechaConfirmacion;
    @Column(name = "fecha_liberacion")
    private LocalDateTime fechaLiberacion;
    @Column(name = "fecha_reembolso")
    private LocalDateTime fechaReembolso;

    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer value) { this.idPago = value; }
    public Integer getIdTrabajo() { return idTrabajo; }
    public void setIdTrabajo(Integer value) { this.idTrabajo = value; }
    public Integer getIdCliente() { return idCliente; }
    public void setIdCliente(Integer value) { this.idCliente = value; }
    public Integer getIdEstudiante() { return idEstudiante; }
    public void setIdEstudiante(Integer value) { this.idEstudiante = value; }
    public UUID getUuidPago() { return uuidPago; }
    public void setUuidPago(UUID value) { this.uuidPago = value; }
    public String getProveedorPago() { return proveedorPago; }
    public void setProveedorPago(String value) { this.proveedorPago = value; }
    public String getEntornoPago() { return entornoPago; }
    public void setEntornoPago(String value) { this.entornoPago = value; }
    public String getMetodoPago() { return metodoPago; }
    public void setMetodoPago(String value) { this.metodoPago = value; }
    public String getMonedaCobro() { return monedaCobro; }
    public void setMonedaCobro(String value) { this.monedaCobro = value; }
    public String getMonedaProcesamiento() { return monedaProcesamiento; }
    public void setMonedaProcesamiento(String value) { this.monedaProcesamiento = value; }
    public BigDecimal getTipoCambio() { return tipoCambio; }
    public void setTipoCambio(BigDecimal value) { this.tipoCambio = value; }
    public BigDecimal getMontoProcesamiento() { return montoProcesamiento; }
    public void setMontoProcesamiento(BigDecimal value) { this.montoProcesamiento = value; }
    public BigDecimal getMontoEstudiante() { return montoEstudiante; }
    public void setMontoEstudiante(BigDecimal value) { this.montoEstudiante = value; }
    public BigDecimal getPorcentajeComisionPlataforma() { return porcentajeComisionPlataforma; }
    public void setPorcentajeComisionPlataforma(BigDecimal value) { this.porcentajeComisionPlataforma = value; }
    public BigDecimal getComisionPlataforma() { return comisionPlataforma; }
    public void setComisionPlataforma(BigDecimal value) { this.comisionPlataforma = value; }
    public BigDecimal getComisionProcesador() { return comisionProcesador; }
    public void setComisionProcesador(BigDecimal value) { this.comisionProcesador = value; }
    public BigDecimal getImpuestoProcesador() { return impuestoProcesador; }
    public void setImpuestoProcesador(BigDecimal value) { this.impuestoProcesador = value; }
    public BigDecimal getMontoTotalCliente() { return montoTotalCliente; }
    public void setMontoTotalCliente(BigDecimal value) { this.montoTotalCliente = value; }
    public String getEstadoPago() { return estadoPago; }
    public void setEstadoPago(String value) { this.estadoPago = value; }
    public String getReferenciaComercio() { return referenciaComercio; }
    public void setReferenciaComercio(String value) { this.referenciaComercio = value; }
    public String getReferenciaProveedor() { return referenciaProveedor; }
    public void setReferenciaProveedor(String value) { this.referenciaProveedor = value; }
    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    public void setFechaCreacion(LocalDateTime value) { this.fechaCreacion = value; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime value) { this.fechaActualizacion = value; }
    public LocalDateTime getFechaExpiracion() { return fechaExpiracion; }
    public void setFechaExpiracion(LocalDateTime value) { this.fechaExpiracion = value; }
    public LocalDateTime getFechaConfirmacion() { return fechaConfirmacion; }
    public void setFechaConfirmacion(LocalDateTime value) { this.fechaConfirmacion = value; }
    public LocalDateTime getFechaLiberacion() { return fechaLiberacion; }
    public void setFechaLiberacion(LocalDateTime value) { this.fechaLiberacion = value; }
    public LocalDateTime getFechaReembolso() { return fechaReembolso; }
    public void setFechaReembolso(LocalDateTime value) { this.fechaReembolso = value; }
}
