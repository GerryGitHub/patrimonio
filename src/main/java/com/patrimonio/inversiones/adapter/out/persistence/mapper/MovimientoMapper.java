package com.patrimonio.inversiones.adapter.out.persistence.mapper;

import com.patrimonio.inversiones.adapter.out.persistence.entity.MovimientoEntity;
import com.patrimonio.inversiones.domain.model.EstadoMovimiento;
import com.patrimonio.inversiones.domain.model.Movimiento;
import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import com.patrimonio.shared.domain.error.DatosPersistidosInvalidos;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Mapper manual entre la entidad JPA y el agregado de movimiento.
 */
@Component
public class MovimientoMapper {

    public Movimiento toDomain(
        MovimientoEntity entity,
        Map<UUID, TipoMovimiento> tipos
    ) {
        TipoMovimiento tipo = tipos.get(entity.getTipoMovimientoId());
        if (tipo == null) {
            throw new DatosPersistidosInvalidos(
                "El movimiento " + entity.getId()
                    + " referencia un tipo de movimiento inexistente.");
        }
        EstadoMovimiento estado;
        try {
            estado = EstadoMovimiento.valueOf(entity.getEstado());
        } catch (IllegalArgumentException ex) {
            throw new DatosPersistidosInvalidos(
                "El movimiento " + entity.getId()
                    + " tiene un estado desconocido: " + entity.getEstado(),
                ex);
        }
        return Movimiento.reconstruir(
            entity.getId(),
            entity.getPortafolioId(),
            entity.getCuentaId(),
            entity.getPosicionId(),
            entity.getInstrumentoId(),
            tipo,
            entity.getFechaOperacion(),
            entity.getFechaLiquidacion(),
            entity.getCantidad(),
            entity.getPrecio(),
            entity.getImporte(),
            entity.getComision(),
            entity.getImpuestos(),
            entity.getMonedaId(),
            entity.getTipoCambio(),
            entity.getImporteNeto(),
            entity.getCostoAplicado(),
            entity.getResultadoRealizado(),
            entity.getEfectoCantidad(),
            entity.getEfectoCosto(),
            entity.getCantidadResultante(),
            entity.getCostoResultante(),
            estado,
            entity.getUsuarioId(),
            entity.getClaveIdempotencia(),
            entity.getMovimientoOrigenId(),
            entity.getDescripcion(),
            entity.getFechaCreacion());
    }

    public MovimientoEntity toEntity(
        Movimiento movimiento,
        UUID tipoMovimientoId
    ) {
        return new MovimientoEntity(
            movimiento.id(),
            movimiento.portafolioId(),
            movimiento.cuentaId(),
            movimiento.posicionId(),
            movimiento.instrumentoId(),
            tipoMovimientoId,
            movimiento.fechaOperacion(),
            movimiento.fechaLiquidacion().orElse(null),
            movimiento.cantidad(),
            movimiento.precioUnitario(),
            movimiento.importeBruto(),
            movimiento.monedaId(),
            movimiento.comisiones(),
            movimiento.impuestos(),
            movimiento.tipoCambio(),
            movimiento.importeNeto(),
            movimiento.costoAplicado(),
            movimiento.resultadoRealizado().orElse(null),
            movimiento.efectoCantidad(),
            movimiento.efectoCosto(),
            movimiento.cantidadResultante(),
            movimiento.costoResultante(),
            movimiento.estado().name(),
            movimiento.usuarioId().orElse(null),
            movimiento.claveIdempotencia().orElse(null),
            movimiento.movimientoOrigenId().orElse(null),
            movimiento.observaciones().orElse(null),
            movimiento.fechaCreacion());
    }
}
