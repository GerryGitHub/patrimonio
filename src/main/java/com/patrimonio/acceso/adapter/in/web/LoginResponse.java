package com.patrimonio.acceso.adapter.in.web;

/**
 * Resultado del inicio de sesión.
 *
 * <p>La credencial de acceso viaja en la cookie de sesión; este cuerpo solo
 * informa del mecanismo, la expiración y el token CSRF que debe usarse en la
 * siguiente petición que modifique estado.</p>
 */
public record LoginResponse(
    String mecanismo,
    UsuarioResponse usuario,
    long expiraEnSegundos,
    long duracionMaximaSegundos,
    String csrfToken
) {
}
