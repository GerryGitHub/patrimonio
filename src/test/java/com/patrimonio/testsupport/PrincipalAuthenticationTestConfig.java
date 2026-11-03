package com.patrimonio.testsupport;

import java.util.List;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registra el resolutor de {@code @AuthenticationPrincipal} en pruebas de
 * rebanada web que desactivan los filtros de seguridad.
 *
 * <p>Sin la cadena de filtros, Spring Security no importa su configuración MVC
 * y el argumento quedaría sin resolver. El resolutor lee el contexto de
 * seguridad del hilo, que la prueba establece explícitamente.</p>
 */
@TestConfiguration
public class PrincipalAuthenticationTestConfig implements WebMvcConfigurer {

    @Override
    public void addArgumentResolvers(
        List<HandlerMethodArgumentResolver> resolvers
    ) {
        resolvers.add(new AuthenticationPrincipalArgumentResolver());
    }
}
