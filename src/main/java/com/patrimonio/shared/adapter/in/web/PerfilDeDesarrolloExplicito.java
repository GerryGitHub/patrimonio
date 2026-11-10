package com.patrimonio.shared.adapter.in.web;

import java.util.Arrays;
import java.util.Set;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Activa adaptadores web de desarrollo solo cuando el perfil {@code local} o
 * {@code test} está activo de forma explícita.
 *
 * <p>{@code @Profile} no sirve para este caso: si no se indica ningún perfil,
 * Spring aplica {@code spring.profiles.default} ({@code local}) y la anotación
 * lo considera activo. Esta condición consulta únicamente
 * {@link org.springframework.core.env.Environment#getActiveProfiles()}, de modo
 * que un arranque sin perfil no registra el adaptador aunque el perfil por
 * defecto sea {@code local} y la propiedad de exposición valga {@code true}.</p>
 */
public final class PerfilDeDesarrolloExplicito implements Condition {

    private static final Set<String> PERFILES_PERMITIDOS = Set.of("local", "test");

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        return Arrays.stream(context.getEnvironment().getActiveProfiles())
            .anyMatch(PERFILES_PERMITIDOS::contains);
    }
}
