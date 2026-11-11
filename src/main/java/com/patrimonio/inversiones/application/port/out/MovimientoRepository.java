package com.patrimonio.inversiones.application.port.out;

import com.patrimonio.inversiones.application.query.FiltroMovimientos;
import com.patrimonio.inversiones.application.query.PaginaMovimientos;
import com.patrimonio.inversiones.domain.model.Movimiento;
import java.time.LocalDate;
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

    /**
     * Historial confirmado de un portafolio en un rango inclusivo.
     *
     * <p>Es una consulta de solo lectura para la consolidación patrimonial.
     * Cualquiera de los extremos puede ser {@code null}. No modifica el motor
     * de movimientos ni recalcula efectos: devuelve los valores almacenados.</p>
     */
    List<Movimiento> listarPorPortafolio(
        UUID portafolioId,
        LocalDate desde,
        LocalDate hasta);

    PaginaMovimientos listarPorPosicion(
        UUID posicionId,
        FiltroMovimientos filtro,
        int pagina,
        int tamano,
        boolean descendente);
}
