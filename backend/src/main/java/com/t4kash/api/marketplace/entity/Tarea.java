package com.t4kash.api.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tareas")
public class Tarea {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tarea")
    private Integer idTarea;

    @Column(name = "titulo", nullable = false, length = 150)
    private String titulo;

    @Column(name = "descripcion", nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(name = "presupuesto", nullable = false, precision = 10, scale = 2)
    private BigDecimal presupuesto;

    @Column(name = "fecha_publicacion", nullable = false)
    private LocalDateTime fechaPublicacion;

    @Column(name = "fecha_limite_postulacion")
    private LocalDateTime fechaLimitePostulacion;

    @Column(name = "fecha_limite")
    private LocalDateTime fechaLimite;

    @Column(name = "estado_tarea", nullable = false, length = 30)
    private String estadoTarea;

    @Column(name = "id_categoria", nullable = false)
    private Integer idCategoria;

    @Column(name = "id_cliente", nullable = false)
    private Integer idCliente;

    @Column(name = "tipo_oportunidad", nullable = false, length = 50)
    private String tipoOportunidad;

    @Column(name = "modalidad", length = 30)
    private String modalidad;

    @Column(name = "visibilidad", nullable = false, length = 30)
    private String visibilidad;

    @Column(name = "direccion_referencia", length = 250)
    private String direccionReferencia;

    @Column(name = "latitud", precision = 9, scale = 6)
    private BigDecimal latitud;

    @Column(name = "longitud", precision = 9, scale = 6)
    private BigDecimal longitud;

    public Integer getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Integer idTarea) {
        this.idTarea = idTarea;
    }

    public String getTitulo() {
        return titulo;
    }

    public void setTitulo(String titulo) {
        this.titulo = titulo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public BigDecimal getPresupuesto() {
        return presupuesto;
    }

    public void setPresupuesto(BigDecimal presupuesto) {
        this.presupuesto = presupuesto;
    }

    public LocalDateTime getFechaPublicacion() {
        return fechaPublicacion;
    }

    public void setFechaPublicacion(LocalDateTime fechaPublicacion) {
        this.fechaPublicacion = fechaPublicacion;
    }

    public LocalDateTime getFechaLimitePostulacion() {
        return fechaLimitePostulacion;
    }

    public void setFechaLimitePostulacion(LocalDateTime fechaLimitePostulacion) {
        this.fechaLimitePostulacion = fechaLimitePostulacion;
    }

    public LocalDateTime getFechaLimite() {
        return fechaLimite;
    }

    public void setFechaLimite(LocalDateTime fechaLimite) {
        this.fechaLimite = fechaLimite;
    }

    public String getEstadoTarea() {
        return estadoTarea;
    }

    public void setEstadoTarea(String estadoTarea) {
        this.estadoTarea = estadoTarea;
    }

    public Integer getIdCategoria() {
        return idCategoria;
    }

    public void setIdCategoria(Integer idCategoria) {
        this.idCategoria = idCategoria;
    }

    public Integer getIdCliente() {
        return idCliente;
    }

    public void setIdCliente(Integer idCliente) {
        this.idCliente = idCliente;
    }

    public String getTipoOportunidad() {
        return tipoOportunidad;
    }

    public void setTipoOportunidad(String tipoOportunidad) {
        this.tipoOportunidad = tipoOportunidad;
    }

    public String getModalidad() {
        return modalidad;
    }

    public void setModalidad(String modalidad) {
        this.modalidad = modalidad;
    }

    public String getVisibilidad() {
        return visibilidad;
    }

    public void setVisibilidad(String visibilidad) {
        this.visibilidad = visibilidad;
    }

    public String getDireccionReferencia() {
        return direccionReferencia;
    }

    public void setDireccionReferencia(String direccionReferencia) {
        this.direccionReferencia = direccionReferencia;
    }

    public BigDecimal getLatitud() {
        return latitud;
    }

    public void setLatitud(BigDecimal latitud) {
        this.latitud = latitud;
    }

    public BigDecimal getLongitud() {
        return longitud;
    }

    public void setLongitud(BigDecimal longitud) {
        this.longitud = longitud;
    }
}
