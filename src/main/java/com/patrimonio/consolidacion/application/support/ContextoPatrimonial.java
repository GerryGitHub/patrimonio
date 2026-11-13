package com.patrimonio.consolidacion.application.support;

import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import java.time.LocalDate;
import java.util.List;

/**
 * Contexto compartido por las consultas patrimoniales.
 *
 * <p>Se carga una sola vez por caso de uso y contiene únicamente los
 * portafolios autorizados. Las consultas derivadas lo reutilizan sin volver a
 * consultar los puertos de otros módulos.</p>
 */
public record ContextoPatrimonial(
    List<PortafolioPatrimonio> portafolios,
    LocalDate hasta
) {

    public List<PosicionPatrimonio> posiciones() {
        return portafolios.stream()
            .flatMap(dato -> dato.posiciones().stream())
            .toList();
    }

    public List<MovimientoPatrimonio> movimientos() {
        return portafolios.stream()
            .flatMap(dato -> dato.movimientos().stream())
            .toList();
    }

    public List<CuentaPatrimonio> cuentas() {
        return portafolios.stream()
            .flatMap(dato -> dato.cuentas().stream())
            .toList();
    }
}
