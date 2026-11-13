package com.patrimonio.consolidacion.application.support;

import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Reglas compartidas de los movimientos patrimoniales.
 */
@Component
public class MovimientosPatrimonio {

    /**
     * Devuelve los movimientos efectivos: excluye las reversiones y los
     * movimientos originales que ya fueron revertidos para evitar doble
     * conteo.
     */
    public List<MovimientoPatrimonio> efectivos(
        List<MovimientoPatrimonio> movimientos
    ) {
        Set<UUID> revertidos = movimientos.stream()
            .map(MovimientoPatrimonio::movimientoOrigenId)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return movimientos.stream()
            .filter(movimiento -> !movimiento.reversion())
            .filter(movimiento -> !revertidos.contains(movimiento.id()))
            .toList();
    }
}
