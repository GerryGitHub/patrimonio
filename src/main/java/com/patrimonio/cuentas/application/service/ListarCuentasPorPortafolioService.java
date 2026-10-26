package com.patrimonio.cuentas.application.service;

import com.patrimonio.acceso.api.port.PortafolioQueryPort;
import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso de consulta de cuentas por portafolio.
 *
 * <p>Primero comprueba que el portafolio exista a través del contrato público
 * de {@code acceso}; después consulta únicamente sus cuentas. La transacción es
 * de solo lectura y se declara aquí, no en el controlador ni en el
 * repositorio.</p>
 */
@Service
public class ListarCuentasPorPortafolioService implements ListarCuentasPorPortafolioUseCase {

    private final PortafolioQueryPort portafolioQueryPort;
    private final CuentaFinancieraRepository cuentaFinancieraRepository;

    public ListarCuentasPorPortafolioService(
        PortafolioQueryPort portafolioQueryPort,
        CuentaFinancieraRepository cuentaFinancieraRepository
    ) {
        this.portafolioQueryPort = portafolioQueryPort;
        this.cuentaFinancieraRepository = cuentaFinancieraRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaFinancieraView> consultar(ListarCuentasPorPortafolioQuery query) {
        Objects.requireNonNull(query, "query");
        UUID portafolioId = query.portafolioId();

        if (portafolioQueryPort.buscarPorId(portafolioId).isEmpty()) {
            throw new NotFoundException("No existe el portafolio indicado.");
        }

        return cuentaFinancieraRepository.listarPorPortafolio(portafolioId).stream()
            .map(ListarCuentasPorPortafolioService::toView)
            .toList();
    }

    private static CuentaFinancieraView toView(CuentaFinanciera cuenta) {
        return new CuentaFinancieraView(
            cuenta.id(),
            cuenta.nombre(),
            cuenta.numeroCuenta().map(NumeroCuenta::valor).orElse(null),
            cuenta.institucionId(),
            cuenta.tipoCuentaId(),
            cuenta.monedaId(),
            cuenta.activo(),
            cuenta.fechaApertura().orElse(null),
            cuenta.fechaCierre().orElse(null)
        );
    }
}
