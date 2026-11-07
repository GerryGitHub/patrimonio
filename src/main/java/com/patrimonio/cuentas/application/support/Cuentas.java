package com.patrimonio.cuentas.application.support;

import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.UUID;

/**
 * Utilidades comunes para resolver cuentas dentro de un portafolio y
 * convertirlas en vistas de aplicación.
 */
public final class Cuentas {

    private Cuentas() {
    }

    /**
     * Busca una cuenta y comprueba que pertenezca al portafolio indicado.
     *
     * <p>Si la cuenta no existe o pertenece a otro portafolio se responde con
     * el mismo error: no se revela la existencia de cuentas ajenas.</p>
     */
    public static CuentaFinanciera enPortafolio(
        CuentaFinancieraRepository repositorio,
        UUID portafolioId,
        UUID cuentaId
    ) {
        return repositorio.buscarPorId(cuentaId)
            .filter(cuenta -> cuenta.portafolioId().equals(portafolioId))
            .orElseThrow(() -> new NotFoundException(
                "No existe la cuenta indicada en el portafolio."));
    }

    public static CuentaFinancieraView vista(CuentaFinanciera cuenta) {
        return new CuentaFinancieraView(
            cuenta.id(),
            cuenta.nombre(),
            cuenta.numeroCuenta().map(NumeroCuenta::valor).orElse(null),
            cuenta.institucionId(),
            cuenta.tipoCuentaId(),
            cuenta.monedaId(),
            cuenta.activo(),
            cuenta.fechaApertura().orElse(null),
            cuenta.fechaCierre().orElse(null),
            cuenta.descripcion().orElse(null));
    }
}
