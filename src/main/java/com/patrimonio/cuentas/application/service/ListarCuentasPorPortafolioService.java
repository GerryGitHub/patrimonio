package com.patrimonio.cuentas.application.service;

import com.patrimonio.acceso.api.dto.PermisoPortafolio;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import com.patrimonio.shared.domain.error.AccesoDenegadoException;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso de consulta de cuentas por portafolio.
 *
 * <p>Primero verifica la membresía del usuario autenticado sobre el
 * portafolio. Si no hay permiso, responde como si el portafolio no existiera
 * para no revelar su existencia; si la membresía existe pero no concede
 * lectura, deniega el acceso. Después consulta únicamente las cuentas del
 * portafolio autorizado. La transacción es de solo lectura y se declara aquí,
 * no en el controlador ni en el repositorio.</p>
 */
@Service
public class ListarCuentasPorPortafolioService
    implements ListarCuentasPorPortafolioUseCase {

    private final AccesoQueryPort accesoQueryPort;
    private final CuentaFinancieraRepository cuentaFinancieraRepository;

    public ListarCuentasPorPortafolioService(
        AccesoQueryPort accesoQueryPort,
        CuentaFinancieraRepository cuentaFinancieraRepository
    ) {
        this.accesoQueryPort = accesoQueryPort;
        this.cuentaFinancieraRepository = cuentaFinancieraRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<CuentaFinancieraView> execute(ListarCuentasPorPortafolioQuery query) {
        Objects.requireNonNull(query, "query");
        UUID portafolioId = query.portafolioId();

        PermisoPortafolio permiso = accesoQueryPort
            .buscarPermiso(query.usuarioId(), portafolioId)
            .orElseThrow(() -> new NotFoundException(
                "No existe el portafolio indicado."));

        if (!permiso.lectura()) {
            throw new AccesoDenegadoException();
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
