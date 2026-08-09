package com.t4kash.api.finance.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "eventos_webhook_pago")
public class EventoWebhookPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_webhook")
    private Long idEventoWebhook;
    @Column(name = "id_pago")
    private Integer idPago;
    @Column(name = "proveedor_pago", nullable = false, length = 30)
    private String proveedorPago;
    @Column(name = "entorno_pago", nullable = false, length = 20)
    private String entornoPago;
    @Column(name = "id_evento_proveedor", nullable = false, length = 180)
    private String idEventoProveedor;
    @Column(name = "tipo_evento", nullable = false, length = 80)
    private String tipoEvento;
    @Column(name = "firma_valida", nullable = false)
    private boolean firmaValida;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> payload;
    @Column(name = "estado_procesamiento", nullable = false, length = 30)
    private String estadoProcesamiento;
    @Column(name = "intentos_procesamiento", nullable = false)
    private Integer intentosProcesamiento;
    @Column(name = "fecha_recepcion", nullable = false)
    private LocalDateTime fechaRecepcion;
    @Column(name = "fecha_procesamiento")
    private LocalDateTime fechaProcesamiento;
    @Column(name = "ultimo_error", length = 700)
    private String ultimoError;

    public void setIdPago(Integer value) { this.idPago = value; }
    public void setProveedorPago(String value) { this.proveedorPago = value; }
    public void setEntornoPago(String value) { this.entornoPago = value; }
    public void setIdEventoProveedor(String value) { this.idEventoProveedor = value; }
    public void setTipoEvento(String value) { this.tipoEvento = value; }
    public void setFirmaValida(boolean value) { this.firmaValida = value; }
    public void setPayload(Map<String, Object> value) { this.payload = value; }
    public void setEstadoProcesamiento(String value) { this.estadoProcesamiento = value; }
    public void setIntentosProcesamiento(Integer value) { this.intentosProcesamiento = value; }
    public void setFechaRecepcion(LocalDateTime value) { this.fechaRecepcion = value; }
    public void setFechaProcesamiento(LocalDateTime value) { this.fechaProcesamiento = value; }
    public void setUltimoError(String value) { this.ultimoError = value; }
}
