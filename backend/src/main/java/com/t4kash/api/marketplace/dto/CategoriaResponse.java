package com.t4kash.api.marketplace.dto;

import com.t4kash.api.marketplace.entity.CategoriaTarea;

public record CategoriaResponse(
        Integer idCategoria,
        String nombreCategoria,
        String descripcion,
        Boolean estado
) {
    public static CategoriaResponse fromEntity(CategoriaTarea categoria) {
        return new CategoriaResponse(
                categoria.getIdCategoria(),
                categoria.getNombreCategoria(),
                categoria.getDescripcion(),
                categoria.getEstado()
        );
    }
}
