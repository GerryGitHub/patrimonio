package com.patrimonio.cuentas.api.port;

import com.patrimonio.cuentas.api.dto.CuentaPatrimonio;
import java.util.List;
import java.util.UUID;

/**
 * Contrato público de consulta de cuentas con institución y tipo resueltos.
 *
 * <p>Complementa a {@link CuentaConsultaPort} sin romper su firma. El
 * consumidor autoriza el portafolio antes de invocar el puerto; este contrato
 * solo devuelve las cuentas que pertenecen al portafolio indicado.</p>
 */
public interface CuentaPatrimonioPort {

    List<CuentaPatrimonio> listarPorPortafolio(UUID portafolioId);
}
