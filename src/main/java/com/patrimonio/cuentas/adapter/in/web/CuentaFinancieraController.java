package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.cuentas.application.command.ActualizarCuentaCommand;
import com.patrimonio.cuentas.application.command.CambiarEstadoCuentaCommand;
import com.patrimonio.cuentas.application.command.CrearCuentaCommand;
import com.patrimonio.cuentas.application.port.in.ActualizarCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.CambiarEstadoCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.ConsultarCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.CrearCuentaUseCase;
import com.patrimonio.cuentas.application.port.in.ListarCuentasPorPortafolioUseCase;
import com.patrimonio.cuentas.application.query.ConsultarCuentaQuery;
import com.patrimonio.cuentas.application.query.ListarCuentasPorPortafolioQuery;
import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import com.patrimonio.shared.adapter.in.web.PerfilDeDesarrolloExplicito;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST para las cuentas financieras.
 *
 * <p>No conoce repositorios ni entidades JPA: solo invoca los puertos de
 * entrada y ensambla la respuesta. El usuario autenticado se toma del contexto
 * de seguridad; nunca de un parámetro enviado por el cliente.</p>
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
    private final ConsultarCuentaUseCase consultarCuenta;
    private final CrearCuentaUseCase crearCuenta;
    private final ActualizarCuentaUseCase actualizarCuenta;
    private final CambiarEstadoCuentaUseCase cambiarEstadoCuenta;
    private final CuentaFinancieraResponseAssembler assembler;

    public CuentaFinancieraController(
        ListarCuentasPorPortafolioUseCase listarCuentas,
        ConsultarCuentaUseCase consultarCuenta,
        CrearCuentaUseCase crearCuenta,
        ActualizarCuentaUseCase actualizarCuenta,
        CambiarEstadoCuentaUseCase cambiarEstadoCuenta,
        CuentaFinancieraResponseAssembler assembler
    ) {
        this.listarCuentas = listarCuentas;
        this.consultarCuenta = consultarCuenta;
        this.crearCuenta = crearCuenta;
        this.actualizarCuenta = actualizarCuenta;
        this.cambiarEstadoCuenta = cambiarEstadoCuenta;
        this.assembler = assembler;
    }

    @GetMapping
    public ResponseEntity<List<CuentaFinancieraResponse>> listar(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "institucionId", required = false) UUID institucionId,
        @RequestParam(name = "tipoCuentaId", required = false) UUID tipoCuentaId,
        @RequestParam(name = "activo", required = false) Boolean activo,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<CuentaFinancieraView> cuentas = listarCuentas.execute(
            new ListarCuentasPorPortafolioQuery(
                usuario.id(),
                portafolioId,
                institucionId,
                tipoCuentaId,
                activo));

        return ResponseEntity.ok(assembler.toResponses(cuentas));
    }

    @GetMapping("/{cuentaId}")
    public ResponseEntity<CuentaFinancieraResponse> consultar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("cuentaId") UUID cuentaId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        CuentaFinancieraView cuenta = consultarCuenta.execute(
            new ConsultarCuentaQuery(usuario.id(), portafolioId, cuentaId));

        return ResponseEntity.ok(assembler.toResponse(cuenta));
    }

    @PostMapping
    public ResponseEntity<CuentaFinancieraResponse> crear(
        @PathVariable("portafolioId") UUID portafolioId,
        @Valid @RequestBody CrearCuentaRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        CuentaFinancieraView creada = crearCuenta.execute(
            new CrearCuentaCommand(
                usuario.id(),
                portafolioId,
                request.institucionId(),
                request.tipoCuentaId(),
                request.monedaId(),
                request.nombre(),
                request.descripcion(),
                request.numeroCuenta(),
                request.fechaApertura()));

        return ResponseEntity
            .created(URI.create(
                "/api/v1/portafolios/" + portafolioId + "/cuentas/" + creada.id()))
            .body(assembler.toResponse(creada));
    }

    @PutMapping("/{cuentaId}")
    public ResponseEntity<CuentaFinancieraResponse> actualizar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("cuentaId") UUID cuentaId,
        @Valid @RequestBody ActualizarCuentaRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        CuentaFinancieraView actualizada = actualizarCuenta.execute(
            new ActualizarCuentaCommand(
                usuario.id(),
                portafolioId,
                cuentaId,
                request.institucionId(),
                request.tipoCuentaId(),
                request.monedaId(),
                request.nombre(),
                request.descripcion(),
                request.numeroCuenta(),
                request.fechaApertura()));

        return ResponseEntity.ok(assembler.toResponse(actualizada));
    }

    @PatchMapping("/{cuentaId}/estado")
    public ResponseEntity<CuentaFinancieraResponse> cambiarEstado(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("cuentaId") UUID cuentaId,
        @Valid @RequestBody CambiarEstadoCuentaRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        CuentaFinancieraView actualizada = cambiarEstadoCuenta.execute(
            new CambiarEstadoCuentaCommand(
                usuario.id(),
                portafolioId,
                cuentaId,
                request.activo()));

        return ResponseEntity.ok(assembler.toResponse(actualizada));
    }
}
