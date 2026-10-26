package com.patrimonio.cuentas.adapter.in.web;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO de respuesta de una cuenta financiera.
 *
 * <p>No expone entidades JPA ni el {@code portafolioId}, que ya forma parte de
 * la ruta solicitada. Los campos opcionales del esquema se serializan como
 * {@code null}.</p>
 */
public record CuentaFinancieraResponse(
    UUID id,
    String nombre,
    String numeroCuenta,
    UUID institucionId,
    UUID tipoCuentaId,
    UUID monedaId,
    boolean activo,
    LocalDate fechaApertura,
    LocalDate fechaCierre
) {
}
