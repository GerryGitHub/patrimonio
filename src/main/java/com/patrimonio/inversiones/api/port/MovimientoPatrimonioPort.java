package com.patrimonio.inversiones.api.port;

import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Contrato público de consulta de movimientos confirmados por portafolio.
 *
 * <p>Se usa para reconstruir el resultado realizado y la evolución del costo.
 * El rango es inclusivo y cualquiera de sus extremos puede ser {@code null}
 * para no limitar la consulta. El consumidor autoriza el portafolio antes de
 * invocar el puerto.</p>
 */
public interface MovimientoPatrimonioPort {

    List<MovimientoPatrimonio> listarPorPortafolio(
        UUID portafolioId,
        LocalDate desde,
        LocalDate hasta);
}
