package com.patrimonio.inversiones.adapter.out.persistence.mapper;

import com.patrimonio.inversiones.adapter.out.persistence.entity.TipoInstrumentoEntity;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import org.springframework.stereotype.Component;

/**
 * Mapper entre la entidad JPA y el modelo de tipo de instrumento.
 */
@Component
public class TipoInstrumentoMapper {

    public TipoInstrumento toDomain(TipoInstrumentoEntity entity) {
        return new TipoInstrumento(
            entity.getId(),
            entity.getClaseActivoId(),
            entity.getNombre(),
            entity.getDescripcion(),
            entity.isActivo());
    }
}
