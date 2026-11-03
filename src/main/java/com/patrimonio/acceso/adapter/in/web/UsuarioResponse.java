package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import java.util.UUID;

/**
 * Datos mínimos del usuario autenticado que se devuelven por HTTP.
 */
public record UsuarioResponse(
    UUID id,
    String email,
    String nombre
) {

    static UsuarioResponse from(UsuarioAutenticado usuario) {
        return new UsuarioResponse(usuario.id(), usuario.email(), usuario.nombre());
    }
}
