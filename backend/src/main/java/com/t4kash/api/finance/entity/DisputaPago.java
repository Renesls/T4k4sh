package com.t4kash.api.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "disputas_pago")
public class DisputaPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_disputa")
    private Integer idDisputa;
    @Column(name = "id_pago", nullable = false)
    private Integer idPago;
    @Column(name = "id_usuario_abre", nullable = false)
    private Integer idUsuarioAbre;
    @Column(name = "id_admin_asignado")
    private Integer idAdminAsignado;
    @Column(name = "motivo", nullable = false, length = 120)
    private String motivo;
    @Column(name = "descripcion", nullable = false, length = 1000)
    private String descripcion;
    @Column(name = "solucion_solicitada", nullable = false, length = 40)
    private String solucionSolicitada;
    @Column(name = "monto_disputado", nullable = false, precision = 12, scale = 2)
    private BigDecimal montoDisputado;
    @Column(name = "estado_disputa", nullable = false, length = 30)
    private String estadoDisputa;
    @Column(name = "prioridad", nullable = false, length = 20)
    private String prioridad;
    @Column(name = "fecha_apertura", nullable = false)
    private LocalDateTime fechaApertura;
    @Column(name = "fecha_limite_respuesta")
    private LocalDateTime fechaLimiteRespuesta;
    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;
    @Column(name = "fecha_resolucion")
    private LocalDateTime fechaResolucion;
    @Column(name = "resolucion", length = 1000)
    private String resolucion;

    public Integer getIdDisputa() { return idDisputa; }
    public void setIdDisputa(Integer value) { this.idDisputa = value; }
    public Integer getIdPago() { return idPago; }
    public void setIdPago(Integer value) { this.idPago = value; }
    public Integer getIdUsuarioAbre() { return idUsuarioAbre; }
    public void setIdUsuarioAbre(Integer value) { this.idUsuarioAbre = value; }
    public Integer getIdAdminAsignado() { return idAdminAsignado; }
    public void setIdAdminAsignado(Integer value) { this.idAdminAsignado = value; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String value) { this.motivo = value; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String value) { this.descripcion = value; }
    public String getSolucionSolicitada() { return solucionSolicitada; }
    public void setSolucionSolicitada(String value) { this.solucionSolicitada = value; }
    public BigDecimal getMontoDisputado() { return montoDisputado; }
    public void setMontoDisputado(BigDecimal value) { this.montoDisputado = value; }
    public String getEstadoDisputa() { return estadoDisputa; }
    public void setEstadoDisputa(String value) { this.estadoDisputa = value; }
    public String getPrioridad() { return prioridad; }
    public void setPrioridad(String value) { this.prioridad = value; }
    public LocalDateTime getFechaApertura() { return fechaApertura; }
    public void setFechaApertura(LocalDateTime value) { this.fechaApertura = value; }
    public LocalDateTime getFechaLimiteRespuesta() { return fechaLimiteRespuesta; }
    public void setFechaLimiteRespuesta(LocalDateTime value) { this.fechaLimiteRespuesta = value; }
    public LocalDateTime getFechaActualizacion() { return fechaActualizacion; }
    public void setFechaActualizacion(LocalDateTime value) { this.fechaActualizacion = value; }
    public LocalDateTime getFechaResolucion() { return fechaResolucion; }
    public void setFechaResolucion(LocalDateTime value) { this.fechaResolucion = value; }
    public String getResolucion() { return resolucion; }
    public void setResolucion(String value) { this.resolucion = value; }
}
