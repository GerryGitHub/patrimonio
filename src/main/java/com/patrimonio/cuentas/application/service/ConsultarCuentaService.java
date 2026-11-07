package com.patrimonio.cuentas.application.service;

import com.patrimonio.cuentas.application.port.in.ConsultarCuentaUseCase;
import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.application.query.ConsultarCuentaQuery;
import com.patrimonio.cuentas.application.support.AccesoPortafolio;
import com.patrimonio.cuentas.application.support.Cuentas;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación de la consulta del detalle de una cuenta.
 *
 * <p>La cuenta debe pertenecer al portafolio indicado en la ruta y el usuario
 * debe tener lectura sobre ese portafolio. Una cuenta de otro portafolio se
 * responde como inexistente.</p>
 */
@Service
public class ConsultarCuentaService implements ConsultarCuentaUseCase {

    private final AccesoPortafolio accesoPortafolio;
    private final CuentaFinancieraRepository cuentas;

    public ConsultarCuentaService(
        AccesoPortafolio accesoPortafolio,
        CuentaFinancieraRepository cuentas
    ) {
        this.accesoPortafolio = accesoPortafolio;
        this.cuentas = cuentas;
    }

    @Override
    @Transactional(readOnly = true)
    public CuentaFinancieraView execute(ConsultarCuentaQuery query) {
        Objects.requireNonNull(query, "query");
        accesoPortafolio.exigirLectura(query.usuarioId(), query.portafolioId());

        return Cuentas.vista(
            Cuentas.enPortafolio(cuentas, query.portafolioId(), query.cuentaId()));
    }
}
