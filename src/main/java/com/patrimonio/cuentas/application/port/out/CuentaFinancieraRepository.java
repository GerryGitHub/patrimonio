package com.patrimonio.cuentas.application.port.out;

import com.patrimonio.cuentas.application.query.FiltroCuentas;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida de cuentas financieras.
 *
 * <p>Devuelve agregados de dominio, nunca entidades JPA. Las cuentas no se
 * eliminan: el estado se cambia mediante {@link #guardar(CuentaFinanciera)}.</p>
 */
public interface CuentaFinancieraRepository {

    List<CuentaFinanciera> listarPorPortafolio(UUID portafolioId);

    List<CuentaFinanciera> listarPorPortafolio(
        UUID portafolioId,
        FiltroCuentas filtro);

    Optional<CuentaFinanciera> buscarPorId(UUID cuentaId);

    /**
     * Indica si el número ya existe en la institución, excluyendo la cuenta
     * indicada cuando se está editando.
     */
    boolean existeNumeroEnInstitucion(
        UUID institucionId,
        String numeroCuenta,
        UUID excluirCuentaId);

    CuentaFinanciera guardar(CuentaFinanciera cuenta);
}
