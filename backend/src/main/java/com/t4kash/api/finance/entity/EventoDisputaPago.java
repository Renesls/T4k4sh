package com.t4kash.api.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "eventos_disputa_pago")
public class EventoDisputaPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_disputa")
    private Long idEventoDisputa;
    @Column(name = "id_disputa", nullable = false)
    private Integer idDisputa;
    @Column(name = "id_usuario")
    private Integer idUsuario;
    @Column(name = "tipo_evento", nullable = false, length = 50)
    private String tipoEvento;
    @Column(name = "estado_anterior", length = 30)
    private String estadoAnterior;
    @Column(name = "estado_nuevo", length = 30)
    private String estadoNuevo;
    @Column(name = "detalle", length = 1000)
    private String detalle;
    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    public void setIdDisputa(Integer value) { this.idDisputa = value; }
    public void setIdUsuario(Integer value) { this.idUsuario = value; }
    public void setTipoEvento(String value) { this.tipoEvento = value; }
    public void setEstadoAnterior(String value) { this.estadoAnterior = value; }
    public void setEstadoNuevo(String value) { this.estadoNuevo = value; }
    public void setDetalle(String value) { this.detalle = value; }
    public void setFechaEvento(LocalDateTime value) { this.fechaEvento = value; }
}
