package com.t4kash.api.moderation.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "reportes")
public class Reporte {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_reporte")
    private Integer idReporte;

    @Column(name = "id_usuario_reporta", nullable = false)
    private Integer idUsuarioReporta;

    @Column(name = "id_usuario_reportado")
    private Integer idUsuarioReportado;

    @Column(name = "id_tarea")
    private Integer idTarea;

    @Column(name = "motivo", nullable = false, length = 150)
    private String motivo;

    @Column(name = "descripcion", length = 700)
    private String descripcion;

    @Column(name = "estado_reporte", nullable = false, length = 30)
    private String estadoReporte;

    @Column(name = "fecha_reporte", nullable = false)
    private LocalDateTime fechaReporte;

    @Column(name = "id_trabajo")
    private Integer idTrabajo;

    @Column(name = "id_entrega")
    private Integer idEntrega;

    @Column(name = "id_pago")
    private Integer idPago;

    @Column(name = "tipo_reporte", nullable = false, length = 30)
    private String tipoReporte;

    @Column(name = "categoria_reporte", nullable = false, length = 80)
    private String categoriaReporte;

    public Integer getIdReporte() {
        return idReporte;
    }

    public void setIdReporte(Integer idReporte) {
        this.idReporte = idReporte;
    }

    public Integer getIdUsuarioReporta() {
        return idUsuarioReporta;
    }

    public void setIdUsuarioReporta(Integer idUsuarioReporta) {
        this.idUsuarioReporta = idUsuarioReporta;
    }

    public Integer getIdUsuarioReportado() {
        return idUsuarioReportado;
    }

    public void setIdUsuarioReportado(Integer idUsuarioReportado) {
        this.idUsuarioReportado = idUsuarioReportado;
    }

    public Integer getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Integer idTarea) {
        this.idTarea = idTarea;
    }

    public String getMotivo() {
        return motivo;
    }

    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getEstadoReporte() {
        return estadoReporte;
    }

    public void setEstadoReporte(String estadoReporte) {
        this.estadoReporte = estadoReporte;
    }

    public LocalDateTime getFechaReporte() {
        return fechaReporte;
    }

    public void setFechaReporte(LocalDateTime fechaReporte) {
        this.fechaReporte = fechaReporte;
    }

    public Integer getIdTrabajo() {
        return idTrabajo;
    }

    public void setIdTrabajo(Integer idTrabajo) {
        this.idTrabajo = idTrabajo;
    }

    public Integer getIdEntrega() {
        return idEntrega;
    }

    public void setIdEntrega(Integer idEntrega) {
        this.idEntrega = idEntrega;
    }

    public Integer getIdPago() {
        return idPago;
    }

    public void setIdPago(Integer idPago) {
        this.idPago = idPago;
    }

    public String getTipoReporte() {
        return tipoReporte;
    }

    public void setTipoReporte(String tipoReporte) {
        this.tipoReporte = tipoReporte;
    }

    public String getCategoriaReporte() {
        return categoriaReporte;
    }

    public void setCategoriaReporte(String categoriaReporte) {
        this.categoriaReporte = categoriaReporte;
    }
}
