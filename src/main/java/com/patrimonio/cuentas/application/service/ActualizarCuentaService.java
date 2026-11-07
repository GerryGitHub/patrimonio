package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.command.ActualizarCuentaCommand;
import com.patrimonio.cuentas.application.port.in.ActualizarCuentaUseCase;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la edición de cuentas financieras.
 *
 * <p>El portafolio no cambia. El estado se modifica con su propia operación,
 * de modo que esta edición no puede cerrar ni reactivar una cuenta por
 * accidente.</p>
 *
 * <p>La referencia bancaria solo se devuelve enmascarada por HTTP. Por eso, si
 * la edición no incluye una referencia nueva, se conserva la almacenada en
 * lugar de borrarla de forma accidental.</p>
 */
@Service
public class ActualizarCuentaService implements ActualizarCuentaUseCase {

    private final AccesoPortafolio accesoPortafolio;
    private final CuentaFinancieraRepository cuentas;
    private final ValidadorRelacionesCuenta validador;

    public ActualizarCuentaService(
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
    public CuentaFinancieraView execute(ActualizarCuentaCommand command) {
        Objects.requireNonNull(command, "command");

        accesoPortafolio.exigirEscritura(
            command.usuarioId(), command.portafolioId());

        CuentaFinanciera existente = Cuentas.enPortafolio(
            cuentas, command.portafolioId(), command.cuentaId());

        validador.validar(
            command.institucionId(),
            command.tipoCuentaId(),
            command.monedaId());

        String numero = Textos.referencia(command.numeroCuenta());
        if (numero == null) {
            // La API solo entrega la referencia enmascarada. Si el formulario
            // no envía una nueva, se conserva la existente.
            numero = existente.numeroCuenta()
                .map(NumeroCuenta::valor)
                .orElse(null);
        }
        if (numero != null && cuentas.existeNumeroEnInstitucion(
            command.institucionId(), numero, command.cuentaId())) {
            throw new CuentaDuplicadaException(
                "Ya existe una cuenta con ese número en la institución.");
        }

        CuentaFinanciera actualizada = existente.actualizarDatos(
            command.institucionId(),
            command.tipoCuentaId(),
            Textos.nombre(command.nombre()),
            Textos.opcional(command.descripcion()),
            numero == null ? null : new NumeroCuenta(numero),
            command.monedaId(),
            command.fechaApertura());

        return Cuentas.vista(cuentas.guardar(actualizada));
    }
}
