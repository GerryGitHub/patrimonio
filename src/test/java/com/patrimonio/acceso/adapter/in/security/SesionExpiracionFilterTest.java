package com.patrimonio.acceso.adapter.in.security;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias del límite absoluto de sesión.
 */
class SesionExpiracionFilterTest {

    private static final Duration DURACION = Duration.ofHours(8);

    private final HttpServletRequest request = mock(HttpServletRequest.class);
    private final HttpServletResponse response = mock(HttpServletResponse.class);
    private final FilterChain chain = mock(FilterChain.class);
    private final HttpSession session = mock(HttpSession.class);
    private final SesionExpiracionFilter filter =
        new SesionExpiracionFilter(DURACION);

    @Test
    void cuandoLaSesionSuperaLaDuracionMaxima_laInvalida() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SesionExpiracionFilter.ATRIBUTO_INICIO_SESION))
            .thenReturn(Instant.now().minus(DURACION).minusSeconds(1));

        filter.doFilter(request, response, chain);

        verify(session).invalidate();
        verify(chain).doFilter(request, response);
    }

    @Test
    void cuandoLaSesionEstaDentroDelPlazo_noLaInvalida() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SesionExpiracionFilter.ATRIBUTO_INICIO_SESION))
            .thenReturn(Instant.now());

        filter.doFilter(request, response, chain);

        verify(session, never()).invalidate();
        verify(chain).doFilter(request, response);
    }

    @Test
    void cuandoNoHaySesion_noHaceNada() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void cuandoLaSesionNoTieneMarcaDeInicio_noLaInvalida() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SesionExpiracionFilter.ATRIBUTO_INICIO_SESION))
            .thenReturn(null);

        filter.doFilter(request, response, chain);

        verify(session, never()).invalidate();
        verify(chain).doFilter(request, response);
    }

    @Test
    void cuandoLaSesionYaEstabaInvalidada_continuaLaCadena() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        when(session.getAttribute(SesionExpiracionFilter.ATRIBUTO_INICIO_SESION))
            .thenReturn(Instant.now().minus(DURACION).minusSeconds(1));
        org.mockito.Mockito.doThrow(new IllegalStateException("invalidada"))
            .when(session).invalidate();

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }
}
