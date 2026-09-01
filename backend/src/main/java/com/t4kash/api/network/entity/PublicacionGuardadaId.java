package com.t4kash.api.network.entity;

import java.io.Serializable;
import java.util.Objects;

public class PublicacionGuardadaId implements Serializable {
    private Integer idPublicacion;
    private Integer idUsuario;

    public PublicacionGuardadaId() {
    }

    public PublicacionGuardadaId(Integer idPublicacion, Integer idUsuario) {
        this.idPublicacion = idPublicacion;
        this.idUsuario = idUsuario;
    }

    @Override
    public boolean equals(Object value) {
        if (this == value) {
            return true;
        }
        if (!(value instanceof PublicacionGuardadaId other)) {
            return false;
        }
        return Objects.equals(idPublicacion, other.idPublicacion)
                && Objects.equals(idUsuario, other.idUsuario);
    }

    @Override
    public int hashCode() {
        return Objects.hash(idPublicacion, idUsuario);
    }
}
