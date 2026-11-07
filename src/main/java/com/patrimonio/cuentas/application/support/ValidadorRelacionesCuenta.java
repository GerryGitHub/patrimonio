package com.patrimonio.cuentas.application.support;

import com.patrimonio.acceso.api.port.MonedaCatalogoPort;
import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.application.port.out.TipoCuentaRepository;
import com.patrimonio.cuentas.domain.error.CuentaInvalidaException;
import com.patrimonio.cuentas.domain.error.MonedaInvalidaException;
import com.patrimonio.cuentas.domain.error.TipoCuentaInvalidoException;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Valida las relaciones de una cuenta con los catálogos existentes.
 *
 * <p>Una cuenta solo puede apuntar a una institución activa, un tipo de cuenta
 * activo y una moneda activa. La institución es global; no se exige membresía
 * sobre ella, solo sobre el portafolio de la cuenta.</p>
 */
@Component
public class ValidadorRelacionesCuenta {

    private final InstitucionRepository instituciones;
    private final TipoCuentaRepository tiposCuenta;
    private final MonedaCatalogoPort monedas;

    public ValidadorRelacionesCuenta(
        InstitucionRepository instituciones,
        TipoCuentaRepository tiposCuenta,
        MonedaCatalogoPort monedas
    ) {
        this.instituciones = instituciones;
        this.tiposCuenta = tiposCuenta;
        this.monedas = monedas;
    }

    public void validar(UUID institucionId, UUID tipoCuentaId, UUID monedaId) {
        if (institucionId == null) {
            throw new CuentaInvalidaException(
                "La institución de la cuenta es obligatoria.");
        }
        boolean institucionActiva = instituciones.buscarPorId(institucionId)
            .filter(institucion -> institucion.activo())
            .isPresent();
        if (!institucionActiva) {
            throw new CuentaInvalidaException(
                "La institución indicada no existe o no está activa.");
        }

        if (tipoCuentaId == null) {
            throw new CuentaInvalidaException(
                "El tipo de cuenta es obligatorio.");
        }
        if (tiposCuenta.buscarActivoPorId(tipoCuentaId).isEmpty()) {
            throw new TipoCuentaInvalidoException(
                "El tipo de cuenta indicado no existe o no está activo.");
        }

        if (monedaId == null) {
            throw new CuentaInvalidaException(
                "La moneda de la cuenta es obligatoria.");
        }
        if (monedas.buscarActivaPorId(monedaId).isEmpty()) {
            throw new MonedaInvalidaException();
        }
    }
}
