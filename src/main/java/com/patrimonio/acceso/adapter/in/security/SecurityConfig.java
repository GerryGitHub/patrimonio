package com.patrimonio.acceso.adapter.in.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

/**
 * Configuración de seguridad HTTP del backend.
 *
 * <p>Estrategia del MVP: sesión de servidor con cookie {@code HttpOnly} y
 * token CSRF en cookie legible por el cliente. Spring Security autentica la
 * petición; la autorización por membresía se resuelve en los casos de uso.
 * No se habilitan HTTP Basic ni formulario de login.</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityContextRepository securityContextRepository() {
        return new HttpSessionSecurityContextRepository();
    }

    @Bean
    public CsrfTokenRepository csrfTokenRepository(
        @Value("${server.servlet.session.cookie.secure:false}") boolean cookieSegura
    ) {
        CookieCsrfTokenRepository repository =
            CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie
            .secure(cookieSegura)
            .sameSite("Lax")
            .path("/")
            .httpOnly(false));
        return repository;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
        HttpSecurity http,
        SecurityContextRepository securityContextRepository,
        CsrfTokenRepository csrfTokenRepository,
        RestAuthenticationEntryPoint authenticationEntryPoint,
        RestAccessDeniedHandler accessDeniedHandler,
        @Value("${patrimonio.seguridad.sesion.duracion-maxima:8h}")
        Duration duracionMaximaSesion
    ) throws Exception {
        CsrfTokenRequestAttributeHandler csrfHandler =
            new CsrfTokenRequestAttributeHandler();
        // El cliente SPA/móvil envía el token de la cookie tal cual en la
        // cabecera; no se aplica el enmascarado XOR pensado para formularios.
        csrfHandler.setCsrfRequestAttributeName(null);

        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(csrfTokenRepository)
                .csrfTokenRequestHandler(csrfHandler))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/registro").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers("/error").permitAll()
                .anyRequest().authenticated())
            .sessionManagement(session -> session
                .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
            .securityContext(context -> context
                .securityContextRepository(securityContextRepository)
                .requireExplicitSave(true))
            .requestCache(cache -> cache.disable())
            .exceptionHandling(exceptions -> exceptions
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler))
            .httpBasic(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .logout(AbstractHttpConfigurer::disable);

        http.addFilterBefore(
            new SesionExpiracionFilter(duracionMaximaSesion),
            SecurityContextHolderFilter.class);

        return http.build();
    }
}
