package com.patrimonio.cuentas.application.view;

import com.patrimonio.cuentas.domain.model.TipoCuenta;
import java.util.UUID;

/**
 * Vista de aplicación de un tipo de cuenta.
 */
public record TipoCuentaView(
    UUID id,
    String nombre,
    String descripcion,
    boolean activo
) {

    public static TipoCuentaView from(TipoCuenta tipoCuenta) {
        return new TipoCuentaView(
            tipoCuenta.id(),
            tipoCuenta.nombre(),
            tipoCuenta.descripcion().orElse(null),
            tipoCuenta.activo());
    }
}
