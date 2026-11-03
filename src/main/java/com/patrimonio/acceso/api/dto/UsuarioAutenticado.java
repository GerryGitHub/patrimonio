package com.patrimonio.acceso.api.dto;

import java.io.Serializable;
import java.util.UUID;

/**
 * Identidad mínima del usuario autenticado.
 *
 * <p>Es el principal que se guarda en la sesión. No contiene contraseña,
 * hash, roles globales ni datos de membresía. El identificador es el que usa
 * la aplicación para autorizar el acceso a cada portafolio.</p>
 */
public record UsuarioAutenticado(
    UUID id,
    String email,
    String nombre
) implements Serializable {
}
