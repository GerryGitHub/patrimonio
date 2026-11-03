package com.patrimonio.cuentas.adapter.in.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Impide habilitar el endpoint de cuentas con el perfil {@code prod}.
 *
 * <p>La propiedad {@code patrimonio.api.cuentas.expuesta} es un opt-in
 * explícito y vale {@code false} por defecto. Aun así, esta comprobación falla
 * el arranque si alguien la activa en producción, antes de que el servidor
 * acepte tráfico. El endpoint no debe exponerse hasta que exista autenticación
 * y autorización por membresía.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
@ConditionalOnProperty(
    prefix = "patrimonio.api.cuentas",
    name = "expuesta",
    havingValue = "true"
)
class CuentasApiExposicionGuard {

    CuentasApiExposicionGuard() {
        throw new IllegalStateException(
            "El endpoint de cuentas no puede exponerse con el perfil 'prod': "
                + "requiere autenticacion y autorizacion por membresia.");
    }
}
