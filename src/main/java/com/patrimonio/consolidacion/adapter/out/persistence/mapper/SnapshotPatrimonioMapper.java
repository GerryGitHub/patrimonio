package com.patrimonio.consolidacion.adapter.out.persistence.mapper;

import com.patrimonio.consolidacion.adapter.out.persistence.entity.SnapshotPatrimonioEntity;
import com.patrimonio.consolidacion.domain.model.SnapshotPatrimonio;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre el snapshot de dominio y su entidad JPA.
 */
@Component
public class SnapshotPatrimonioMapper {

    public SnapshotPatrimonio toDomain(SnapshotPatrimonioEntity entity) {
        return SnapshotPatrimonio.reconstruir(
            entity.getId(),
            entity.getPortafolioId(),
            entity.getFecha(),
            entity.getMonedaId(),
            entity.getCapitalInvertido(),
            entity.getResultadoRealizado(),
            entity.getEfectivo(),
            entity.getPasivos(),
            entity.getValorMercado(),
            entity.getPatrimonioNeto(),
            entity.getCobertura(),
            entity.getLimitaciones(),
            entity.getFuente(),
            entity.getFechaCreacion());
    }

    public SnapshotPatrimonioEntity toEntity(SnapshotPatrimonio snapshot) {
        return new SnapshotPatrimonioEntity(
            snapshot.id(),
            snapshot.portafolioId(),
            snapshot.fecha(),
            snapshot.monedaId(),
            snapshot.capitalInvertido(),
            snapshot.resultadoRealizado(),
            snapshot.efectivo(),
            snapshot.pasivos(),
            snapshot.valorMercado(),
            snapshot.patrimonioNeto(),
            snapshot.cobertura(),
            snapshot.limitaciones(),
            snapshot.fuente(),
            snapshot.fechaCreacion());
    }
}
