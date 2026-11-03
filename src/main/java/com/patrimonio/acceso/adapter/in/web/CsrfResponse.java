package com.patrimonio.acceso.adapter.in.web;

/**
 * Token CSRF que el cliente debe enviar en la cabecera indicada.
 */
public record CsrfResponse(
    String headerName,
    String parameterName,
    String token
) {
}
