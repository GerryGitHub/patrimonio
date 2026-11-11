package com.patrimonio.consolidacion.adapter.in.web;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Impide habilitar los endpoints de consolidación con el perfil {@code prod}.
 *
 * <p>La propiedad {@code patrimonio.api.consolidacion.expuesta} es un opt-in
 * explícito y vale {@code false} por defecto. Aun así, esta comprobación falla
 * el arranque si alguien la activa en producción, antes de que el servidor
 * acepte tráfico. Los endpoints todavía requieren autenticación y autorización
 * por membresía.</p>
 */
@Configuration(proxyBeanMethods = false)
@Profile("prod")
@ConditionalOnProperty(
    prefix = "patrimonio.api.consolidacion",
    name = "expuesta",
    havingValue = "true"
)
class ConsolidacionApiExposicionGuard {

    ConsolidacionApiExposicionGuard() {
        throw new IllegalStateException(
            "Los endpoints de consolidación no pueden exponerse con el perfil 'prod': "
                + "requieren autenticacion y autorizacion por membresia.");
    }
}
