package com.patrimonio.inversiones.adapter.in.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Impide habilitar los endpoints de inversiones con el perfil {@code prod}.
 *
 * <p>La propiedad {@code patrimonio.api.inversiones.expuesta} es un opt-in
 * explícito y vale {@code false} por defecto. Aun así, esta comprobación falla
 * el arranque si alguien la activa en producción, antes de que el servidor
 * acepte tráfico.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
@ConditionalOnProperty(
    prefix = "patrimonio.api.inversiones",
    name = "expuesta",
    havingValue = "true"
)
class InversionesApiExposicionGuard {

    InversionesApiExposicionGuard() {
        throw new IllegalStateException(
            "Los endpoints de inversiones no pueden exponerse con el perfil 'prod': "
                + "requieren autenticacion y autorizacion por membresia.");
    }
}
