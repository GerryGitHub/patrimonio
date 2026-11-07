package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.command.CrearCuentaCommand;
import com.patrimonio.cuentas.application.port.in.CrearCuentaUseCase;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.support.AccesoPortafolio;
import com.patrimonio.cuentas.application.support.Cuentas;
import com.patrimonio.cuentas.application.support.Textos;
import com.patrimonio.cuentas.application.support.ValidadorRelacionesCuenta;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.error.CuentaDuplicadaException;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del alta de cuentas financieras.
 *
 * <p>Exige escritura sobre el portafolio, valida las relaciones con los
 * catálogos y evita duplicar el mismo número dentro de una institución. El
 * número completo se conserva para uso interno, pero la API solo devuelve la
 * versión enmascarada.</p>
 */
@Service
public class CrearCuentaService implements CrearCuentaUseCase {

    private final AccesoPortafolio accesoPortafolio;
    private final CuentaFinancieraRepository cuentas;
    private final ValidadorRelacionesCuenta validador;

    public CrearCuentaService(
        AccesoPortafolio accesoPortafolio,
        CuentaFinancieraRepository cuentas,
        ValidadorRelacionesCuenta validador
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
        this.validador = validador;
    }

    @Override
    @Transactional
    public CuentaFinancieraView execute(CrearCuentaCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        validador.validar(
            command.institucionId(),
            command.tipoCuentaId(),
            command.monedaId());

        String numero = Textos.referencia(command.numeroCuenta());
        if (numero != null && cuentas.existeNumeroEnInstitucion(
            command.institucionId(), numero, null)) {
            throw new CuentaDuplicadaException(
                "Ya existe una cuenta con ese número en la institución.");
        }

        CuentaFinanciera cuenta = CuentaFinanciera.crear(
            UUID.randomUUID(),
            command.portafolioId(),
            command.institucionId(),
            command.tipoCuentaId(),
            Textos.nombre(command.nombre()),
            Textos.opcional(command.descripcion()),
            numero == null ? null : new NumeroCuenta(numero),
            command.monedaId(),
            command.fechaApertura());

        return Cuentas.vista(cuentas.guardar(cuenta));
    }
}
