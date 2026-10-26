package com.patrimonio.cuentas.application.port.out;

import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import java.util.List;
import java.util.UUID;

/**
 * Puerto de salida para consultar cuentas financieras.
 *
 * <p>No expone operaciones de escritura en esta fase. Devuelve agregados de
 * dominio, nunca entidades JPA.</p>
 */
public interface CuentaFinancieraRepository {

    List<CuentaFinanciera> listarPorPortafolio(UUID portafolioId);
}
