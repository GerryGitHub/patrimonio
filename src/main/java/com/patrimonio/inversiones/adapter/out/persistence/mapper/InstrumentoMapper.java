package com.patrimonio.inversiones.adapter.out.persistence.mapper;

import com.patrimonio.inversiones.adapter.out.persistence.entity.InstrumentoEntity;
import com.patrimonio.inversiones.domain.model.Instrumento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre la entidad JPA y el agregado de instrumento.
 *
 * <p>El ISIN se recibe resuelto desde {@code identificador_instrumento}; la
 * entidad de instrumento no lo contiene.</p>
 */
@Component
public class InstrumentoMapper {

    public Instrumento toDomain(InstrumentoEntity entity, String isin) {
        try {
            return Instrumento.reconstruir(
                entity.getId(),
                entity.getTipoInstrumentoId(),
                entity.getNombre(),
                entity.getSimbolo(),
                entity.getEmisor(),
                entity.getMercado(),
                entity.getMonedaId(),
                entity.getIdentificadorExterno(),
                isin,
                entity.getDescripcion(),
                entity.isActivo(),
                entity.getFechaCreacion(),
                entity.getFechaActualizacion());
        } catch (DomainException ex) {
            throw new DatosPersistidosInvalidos(
                "El instrumento almacenado no cumple las invariantes de dominio.",
                ex);
        }
    }

    public InstrumentoEntity toEntity(Instrumento instrumento) {
        return new InstrumentoEntity(
            instrumento.id(),
            instrumento.tipoInstrumentoId(),
            instrumento.nombre(),
            instrumento.simbolo().orElse(null),
            instrumento.emisor().orElse(null),
            instrumento.mercado().orElse(null),
            instrumento.monedaId(),
            instrumento.identificadorExterno().orElse(null),
            instrumento.descripcion().orElse(null),
            instrumento.activo(),
            instrumento.fechaCreacion(),
            instrumento.fechaActualizacion());
    }

    public void actualizarEntidad(
        InstrumentoEntity entity,
        Instrumento instrumento
    ) {
        entity.actualizar(
            instrumento.tipoInstrumentoId(),
            instrumento.nombre(),
            instrumento.simbolo().orElse(null),
            instrumento.emisor().orElse(null),
            instrumento.mercado().orElse(null),
            instrumento.monedaId(),
            instrumento.identificadorExterno().orElse(null),
            instrumento.descripcion().orElse(null),
            instrumento.activo(),
            instrumento.fechaActualizacion());
    }
}
