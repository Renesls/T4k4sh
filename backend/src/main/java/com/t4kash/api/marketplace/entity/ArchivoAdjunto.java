package com.t4kash.api.marketplace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "archivos_adjuntos")
public class ArchivoAdjunto {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_archivo")
    private Integer idArchivo;

    @Column(name = "id_tarea")
    private Integer idTarea;

    @Column(name = "id_entrega")
    private Integer idEntrega;

    @Column(name = "id_usuario_sube", nullable = false)
    private Integer idUsuarioSube;

    @Column(name = "nombre_original", nullable = false, length = 255)
    private String nombreOriginal;

    @Column(name = "tipo_mime", nullable = false, length = 100)
    private String tipoMime;

    @Column(name = "extension", length = 20)
    private String extension;

    @Column(name = "tamano_bytes", nullable = false)
    private Long tamanoBytes;

    @Column(name = "bucket_storage", nullable = false, length = 100)
    private String bucketStorage;

    @Column(name = "ruta_storage", nullable = false, length = 500)
    private String rutaStorage;

    @Column(name = "fecha_subida", nullable = false)
    private LocalDateTime fechaSubida;

    @Column(name = "estado_archivo", nullable = false, length = 30)
    private String estadoArchivo;

    public Integer getIdArchivo() {
        return idArchivo;
    }

    public void setIdArchivo(Integer idArchivo) {
        this.idArchivo = idArchivo;
    }

    public Integer getIdTarea() {
        return idTarea;
    }

    public void setIdTarea(Integer idTarea) {
        this.idTarea = idTarea;
    }

    public Integer getIdEntrega() {
        return idEntrega;
    }

    public void setIdEntrega(Integer idEntrega) {
        this.idEntrega = idEntrega;
    }

    public Integer getIdUsuarioSube() {
        return idUsuarioSube;
    }

    public void setIdUsuarioSube(Integer idUsuarioSube) {
        this.idUsuarioSube = idUsuarioSube;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public void setNombreOriginal(String nombreOriginal) {
        this.nombreOriginal = nombreOriginal;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public void setTipoMime(String tipoMime) {
        this.tipoMime = tipoMime;
    }

    public String getExtension() {
        return extension;
    }

    public void setExtension(String extension) {
        this.extension = extension;
    }

    public Long getTamanoBytes() {
        return tamanoBytes;
    }

    public void setTamanoBytes(Long tamanoBytes) {
        this.tamanoBytes = tamanoBytes;
    }

    public String getBucketStorage() {
        return bucketStorage;
    }

    public void setBucketStorage(String bucketStorage) {
        this.bucketStorage = bucketStorage;
    }

    public String getRutaStorage() {
        return rutaStorage;
    }

    public void setRutaStorage(String rutaStorage) {
        this.rutaStorage = rutaStorage;
    }

    public LocalDateTime getFechaSubida() {
        return fechaSubida;
    }

    public void setFechaSubida(LocalDateTime fechaSubida) {
        this.fechaSubida = fechaSubida;
    }

    public String getEstadoArchivo() {
        return estadoArchivo;
    }

    public void setEstadoArchivo(String estadoArchivo) {
        this.estadoArchivo = estadoArchivo;
    }
}
