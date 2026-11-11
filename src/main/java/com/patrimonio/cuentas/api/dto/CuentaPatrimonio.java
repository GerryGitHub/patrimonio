package com.patrimonio.cuentas.api.dto;

import java.util.UUID;

/**
 * Cuenta financiera publicada para la consolidación patrimonial.
 *
 * <p>Incluye el nombre de la institución y del tipo de cuenta para que el
 * módulo consumidor pueda agrupar la distribución sin consultar los catálogos
 * internos de {@code cuentas}. No expone el número de cuenta ni datos
 * sensibles. El saldo de efectivo no forma parte de este contrato porque el
 * módulo de efectivo todavía no existe.</p>
 */
public record CuentaPatrimonio(
    UUID id,
    UUID portafolioId,
    String nombre,
    UUID institucionId,
    String institucionNombre,
    UUID tipoCuentaId,
    String tipoCuentaNombre,
    UUID monedaId,
    boolean activo
) {
}
