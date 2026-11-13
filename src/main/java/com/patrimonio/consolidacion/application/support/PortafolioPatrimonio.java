package com.patrimonio.consolidacion.application.support;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import com.patrimonio.inversiones.api.dto.MovimientoPatrimonio;
import com.patrimonio.inversiones.api.dto.PosicionPatrimonio;
import java.util.List;

/**
 * Datos de un portafolio autorizado que participan en una consulta
 * patrimonial.
 */
public record PortafolioPatrimonio(
    PortafolioResumen portafolio,
    List<CuentaPatrimonio> cuentas,
    List<PosicionPatrimonio> posiciones,
    List<MovimientoPatrimonio> movimientos
) {
}
