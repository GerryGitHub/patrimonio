package com.patrimonio.inversiones.adapter.out.persistence.mapper;

import com.patrimonio.inversiones.adapter.out.persistence.entity.ClaseActivoEntity;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import org.springframework.stereotype.Component;

/**
 * Mapper entre la entidad JPA y el modelo de clase de activo.
 */
@Component
public class ClaseActivoMapper {

    public ClaseActivo toDomain(ClaseActivoEntity entity) {
        return new ClaseActivo(
            entity.getId(),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.isActivo());
    }
}
