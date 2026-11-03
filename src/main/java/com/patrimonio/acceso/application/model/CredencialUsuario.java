package com.patrimonio.acceso.application.model;

import java.util.UUID;

/**
 * Datos internos necesarios para verificar una credencial.
 *
 * <p>No se serializa ni se devuelve por HTTP. El hash solo se entrega al
 * verificador de contraseñas y nunca se registra en logs.</p>
 */
public record CredencialUsuario(
    UUID id,
    String email,
    String nombre,
    String passwordHash,
    boolean activo
) {
}
