package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para las cuentas financieras.
 *
 * <p>No conoce repositorios ni entidades JPA: solo invoca el puerto de entrada
 * y ensambla la respuesta. El usuario autenticado se toma del contexto de
 * seguridad; nunca de un parámetro enviado por el cliente.</p>
 *
 * <p>El controlador solo se registra si
 * {@code patrimonio.api.cuentas.expuesta=true} y el perfil {@code local} o
 * {@code test} está activo de forma explícita. Un arranque sin perfil no lo
 * registra, aunque Spring aplique {@code spring.profiles.default=local} y la
 * propiedad valga {@code true} en el perfil local. En producción el endpoint no
 * existe (404) y el arranque falla si se intenta habilitar.</p>
 */
@RestController
@RequestMapping("/api/v1/portafolios/{portafolioId}/cuentas")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.cuentas",
    name = "expuesta",
    havingValue = "true"
)
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
        @PathVariable("portafolioId") UUID portafolioId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<CuentaFinancieraView> cuentas = listarCuentas.execute(
            new ListarCuentasPorPortafolioQuery(usuario.id(), portafolioId));

        return ResponseEntity.ok(assembler.toResponses(cuentas));
    }
}
