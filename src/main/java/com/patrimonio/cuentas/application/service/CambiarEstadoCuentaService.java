package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.command.CambiarEstadoCuentaCommand;
import com.patrimonio.cuentas.application.port.in.CambiarEstadoCuentaUseCase;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.support.AccesoPortafolio;
import com.patrimonio.cuentas.application.support.Cuentas;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import java.time.LocalDate;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del cambio de estado de una cuenta.
 *
 * <p>Desactivar fija la fecha de cierre; reactivar la limpia. No se elimina
 * físicamente la cuenta para conservar su historial futuro de operaciones.</p>
 */
@Service
public class CambiarEstadoCuentaService implements CambiarEstadoCuentaUseCase {

    private final AccesoPortafolio accesoPortafolio;
    private final CuentaFinancieraRepository cuentas;

    public CambiarEstadoCuentaService(
        AccesoPortafolio accesoPortafolio,
        CuentaFinancieraRepository cuentas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
    }

    @Override
    @Transactional
    public CuentaFinancieraView execute(CambiarEstadoCuentaCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        CuentaFinanciera existente = Cuentas.enPortafolio(
            cuentas, command.portafolioId(), command.cuentaId());

        LocalDate cierre = command.activo() ? null : LocalDate.now();
        return Cuentas.vista(
            cuentas.guardar(existente.cambiarEstado(command.activo(), cierre)));
    }
}
