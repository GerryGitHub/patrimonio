package com.patrimonio.cuentas.adapter.in.web;

import java.util.UUID;

/**
 * DTO de respuesta de un tipo de cuenta del catálogo.
 */
public record TipoCuentaResponse(
    UUID id,
    String nombre,
    String descripcion,
    boolean activo
) {
}
