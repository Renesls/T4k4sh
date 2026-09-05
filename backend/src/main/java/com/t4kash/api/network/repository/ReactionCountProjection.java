package com.t4kash.api.network.repository;

public interface ReactionCountProjection {
    Integer getIdPublicacion();

    String getTipoReaccion();

    long getTotal();
}
