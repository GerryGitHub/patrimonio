package com.patrimonio.cuentas.api.port;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrato público de consulta de cuentas financieras.
 *
 * <p>Otros módulos lo usan para validar que una cuenta pertenece a un
 * portafolio y para resolver su nombre y tipo sin depender de la persistencia
 * del módulo {@code cuentas}. La autorización por membresía sigue siendo
 * responsabilidad del caso de uso consumidor.</p>
 */
public interface CuentaConsultaPort {

    /**
     * Cuenta por identificador, exista o no en el portafolio indicado.
     */
    Optional<CuentaResumen> buscarPorId(UUID cuentaId);

    /**
     * Cuenta que pertenece al portafolio indicado.
     */
    Optional<CuentaResumen> buscarEnPortafolio(UUID portafolioId, UUID cuentaId);

    /**
     * Cuentas del portafolio, ordenadas por nombre e identificador.
     */
    List<CuentaResumen> listarPorPortafolio(UUID portafolioId);
}
