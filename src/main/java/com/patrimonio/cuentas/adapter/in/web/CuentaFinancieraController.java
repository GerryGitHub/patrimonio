package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para las cuentas financieras.
 *
 * <p>No conoce repositorios ni entidades JPA: solo invoca el puerto de entrada
 * y ensambla la respuesta.</p>
 */
@RestController
@RequestMapping("/api/v1/portafolios/{portafolioId}/cuentas")
public class CuentaFinancieraController {

    private final ListarCuentasPorPortafolioUseCase listarCuentas;
    private final CuentaFinancieraResponseAssembler assembler;

    public CuentaFinancieraController(
        ListarCuentasPorPortafolioUseCase listarCuentas,
        CuentaFinancieraResponseAssembler assembler
    ) {
        this.listarCuentas = listarCuentas;
        this.assembler = assembler;
    }

    @GetMapping
    public ResponseEntity<List<CuentaFinancieraResponse>> listar(
        @PathVariable("portafolioId") UUID portafolioId
    ) {
        List<CuentaFinancieraView> cuentas = listarCuentas.consultar(
            new ListarCuentasPorPortafolioQuery(portafolioId));

        return ResponseEntity.ok(assembler.toResponses(cuentas));
    }
}
