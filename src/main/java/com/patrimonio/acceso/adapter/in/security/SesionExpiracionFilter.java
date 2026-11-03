package com.patrimonio.acceso.adapter.in.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Aplica una duración máxima absoluta a la sesión autenticada.
 *
 * <p>El contenedor ya aplica el tiempo de inactividad. Este filtro añade un
 * límite absoluto independiente de la actividad: transcurrido ese plazo, la
 * sesión se invalida y la siguiente petición protegida recibe {@code 401}.
 * Se ejecuta antes de cargar el contexto de seguridad para que una sesión
 * caducada no llegue a autenticar la petición.</p>
 */
public class SesionExpiracionFilter extends OncePerRequestFilter {

    /**
     * Marca de tiempo en la que se creó la sesión autenticada.
     */
    public static final String ATRIBUTO_INICIO_SESION =
        "patrimonio.sesion.inicio";

    private final Duration duracionMaxima;

    public SesionExpiracionFilter(Duration duracionMaxima) {
        this.duracionMaxima = duracionMaxima;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain filterChain
    ) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        if (session != null) {
            invalidarSiCaduco(session);
        }
        filterChain.doFilter(request, response);
    }

    private void invalidarSiCaduco(HttpSession session) {
        Object inicio = session.getAttribute(ATRIBUTO_INICIO_SESION);
        if (inicio instanceof Instant instante
            && Instant.now().isAfter(instante.plus(duracionMaxima))) {
            try {
                session.invalidate();
            } catch (IllegalStateException yaInvalidada) {
                // La sesión ya había sido invalidada por otra petición.
            }
        }
    }
}
