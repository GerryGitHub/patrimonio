package com.patrimonio.testsupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * Obtiene un token CSRF real mediante {@code GET /api/auth/csrf}.
 *
 * <p>Las pruebas de integración usan el flujo real de cookie y cabecera en
 * lugar de {@code SecurityMockMvcRequestPostProcessors.csrf()}. Ese
 * post-procesador reemplaza por reflexión el repositorio del {@code CsrfFilter}
 * compartido y contamina las peticiones posteriores del mismo contexto.</p>
 */
public final class CsrfTestSupport {

    /**
     * Cabecera que el cliente debe enviar con el token de la cookie.
     */
    public static final String CABECERA = "X-XSRF-TOKEN";

    /**
     * Nombre de la cookie CSRF legible por el cliente.
     */
    public static final String COOKIE = "XSRF-TOKEN";

    private CsrfTestSupport() {
    }

    /**
     * Pide un token nuevo y lo adjunta a la petición como cookie y cabecera.
     */
    public static RequestPostProcessor conCsrf(MockMvc mockMvc) throws Exception {
        TokenCsrf token = obtener(mockMvc);
        return request -> {
            adjuntar(request, token);
            return request;
        };
    }

    /**
     * Solicita un token CSRF y devuelve la cookie y el valor recibidos.
     */
    public static TokenCsrf obtener(MockMvc mockMvc) throws Exception {
        MvcResult resultado = mockMvc.perform(get("/api/auth/csrf"))
            .andExpect(status().isOk())
            .andReturn();

        String valor = JsonPath.read(
            resultado.getResponse().getContentAsString(), "$.token");
        Cookie cookie = resultado.getResponse().getCookie(COOKIE);
        if (cookie == null) {
            throw new IllegalStateException("La respuesta no incluyó la cookie CSRF.");
        }
        return new TokenCsrf(cookie, valor);
    }

    /**
     * Adjunta un token ya obtenido a una petición de MockMvc.
     */
    public static void adjuntar(MockHttpServletRequest request, TokenCsrf token) {
        request.setCookies(token.cookie());
        request.addHeader(CABECERA, token.valor());
    }

    /**
     * Cookie y valor de un token CSRF.
     */
    public record TokenCsrf(Cookie cookie, String valor) {
    }
}
