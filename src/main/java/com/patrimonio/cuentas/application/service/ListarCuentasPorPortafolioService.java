package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.support.AccesoPortafolio;
import com.patrimonio.cuentas.application.support.Cuentas;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso de consulta de cuentas por portafolio.
 *
 * <p>Primero verifica la membresía del usuario autenticado sobre el
 * portafolio. Después consulta únicamente las cuentas del portafolio
 * autorizado, aplicando los filtros opcionales. La transacción es de solo
 * lectura y se declara aquí, no en el controlador ni en el repositorio.</p>
 */
@Service
public class ListarCuentasPorPortafolioService
    implements ListarCuentasPorPortafolioUseCase {

    private final AccesoPortafolio accesoPortafolio;
    private final CuentaFinancieraRepository cuentaFinancieraRepository;

    public ListarCuentasPorPortafolioService(
        AccesoPortafolio accesoPortafolio,
        CuentaFinancieraRepository cuentaFinancieraRepository
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentaFinancieraRepository = cuentaFinancieraRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaFinancieraView> execute(ListarCuentasPorPortafolioQuery query) {
        Objects.requireNonNull(query, "query");

        accesoPortafolio.exigirLectura(query.usuarioId(), query.portafolioId());

        return cuentaFinancieraRepository
            .listarPorPortafolio(query.portafolioId(), query.filtro())
            .stream()
            .map(Cuentas::vista)
            .toList();
    }
}
