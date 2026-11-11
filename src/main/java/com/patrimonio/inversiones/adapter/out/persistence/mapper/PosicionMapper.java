package com.patrimonio.inversiones.adapter.out.persistence.mapper;

import com.patrimonio.inversiones.adapter.out.persistence.entity.PosicionEntity;
import com.patrimonio.inversiones.domain.model.Posicion;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import com.patrimonio.shared.domain.error.DomainException;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre la entidad JPA y el agregado de posición.
 */
@Component
public class PosicionMapper {

    public Posicion toDomain(PosicionEntity entity) {
        try {
            return Posicion.reconstruirCompleta(
                entity.getId(),
                entity.getCuentaId(),
                entity.getInstrumentoId(),
                entity.getCantidad(),
                entity.getPrecioPromedio(),
                entity.getCostoTotal(),
                entity.getResultadoRealizado(),
                entity.getMonedaId(),
                entity.getObservaciones(),
                entity.isActivo(),
                entity.getFechaApertura(),
                entity.getFechaUltimoMovimiento(),
                entity.getFechaCreacion(),
                entity.getFechaActualizacion(),
                entity.getVersion());
        } catch (DomainException ex) {
            throw new DatosPersistidosInvalidos(
                "La posición almacenada no cumple las invariantes de dominio.",
                ex);
        }
    }

    public PosicionEntity toEntity(Posicion posicion) {
        return new PosicionEntity(
            posicion.id(),
            posicion.cuentaId(),
            posicion.instrumentoId(),
            null,
            posicion.cantidad(),
            posicion.costoUnitario(),
            posicion.costoTotal(),
            posicion.resultadoRealizado(),
            posicion.fechaUltimoMovimiento().orElse(null),
            posicion.monedaId(),
            posicion.observaciones().orElse(null),
            posicion.activo(),
            posicion.fechaApertura(),
            posicion.fechaCreacion(),
            posicion.fechaActualizacion());
    }

    public void actualizarEntidad(PosicionEntity entity, Posicion posicion) {
        entity.actualizar(
            posicion.cantidad(),
            posicion.costoUnitario(),
            posicion.costoTotal(),
            posicion.resultadoRealizado(),
            posicion.observaciones().orElse(null),
            posicion.activo(),
            posicion.fechaApertura(),
            posicion.fechaUltimoMovimiento().orElse(null),
            posicion.fechaActualizacion());
    }
}
