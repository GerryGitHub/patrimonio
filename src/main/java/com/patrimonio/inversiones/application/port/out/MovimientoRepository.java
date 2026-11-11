package com.patrimonio.inversiones.application.port.out;

import com.patrimonio.inversiones.application.query.FiltroMovimientos;
import com.patrimonio.inversiones.application.query.PaginaMovimientos;
import com.patrimonio.inversiones.domain.model.Movimiento;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Puerto de salida de movimientos.
 *
 * <p>La aplicación autoriza el portafolio antes de consultar. Los movimientos
 * son inmutables: este puerto solo permite guardar filas nuevas.</p>
 */
public interface MovimientoRepository {

    Movimiento guardar(Movimiento movimiento);

    Optional<Movimiento> buscarPorId(UUID movimientoId);

    Optional<Movimiento> buscarPorClaveIdempotencia(
        UUID portafolioId, String claveIdempotencia);

    Optional<Movimiento> buscarReversionDe(UUID movimientoOrigenId);

    Set<UUID> buscarOrigenesRevertidos(Collection<UUID> movimientoIds);

    boolean existePorPosicion(UUID posicionId);

    List<Movimiento> listarPorPosicion(UUID posicionId);

    PaginaMovimientos listarPorPosicion(
        UUID posicionId,
        FiltroMovimientos filtro,
        int pagina,
        int tamano,
        boolean descendente);
}
