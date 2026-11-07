package com.patrimonio.cuentas.application.port.out;

import com.patrimonio.cuentas.domain.model.TipoCuenta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Puerto de salida del catálogo de tipos de cuenta.
 */
public interface TipoCuentaRepository {

    List<TipoCuenta> listarActivos();

    Optional<TipoCuenta> buscarActivoPorId(UUID tipoCuentaId);

    Optional<TipoCuenta> buscarPorId(UUID tipoCuentaId);
}
