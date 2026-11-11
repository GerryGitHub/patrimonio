package com.patrimonio.inversiones.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.inversiones.application.command.ReconstruirPosicionCommand;
import com.patrimonio.inversiones.application.command.RegistrarMovimientoCommand;
import com.patrimonio.inversiones.application.command.RevertirMovimientoCommand;
import com.patrimonio.inversiones.application.port.in.ConsultarMovimientoUseCase;
import com.patrimonio.inversiones.application.port.in.ListarMovimientosUseCase;
import com.patrimonio.inversiones.application.port.in.ReconstruirPosicionUseCase;
import com.patrimonio.inversiones.application.port.in.RegistrarMovimientoUseCase;
import com.patrimonio.inversiones.application.port.in.ResumenPosicionUseCase;
import com.patrimonio.inversiones.application.port.in.RevertirMovimientoUseCase;
import com.patrimonio.inversiones.application.query.ConsultarMovimientoQuery;
import com.patrimonio.inversiones.application.query.ListarMovimientosQuery;
import com.patrimonio.inversiones.application.query.ResumenPosicionQuery;
import com.patrimonio.inversiones.application.support.TextosInversiones;
import com.patrimonio.inversiones.application.view.MovimientoView;
import com.patrimonio.inversiones.application.view.PaginaMovimientosView;
import com.patrimonio.inversiones.application.view.ReconstruccionView;
import com.patrimonio.inversiones.application.view.ResumenPosicionView;
import com.patrimonio.inversiones.domain.model.TipoMovimiento;
import com.patrimonio.shared.adapter.in.web.PerfilDeDesarrolloExplicito;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST de los movimientos de inversión.
 *
 * <p>El historial y el alta cuelgan de una posición; el detalle y la reversión
 * cuelgan del movimiento dentro del portafolio. La autorización por membresía
 * se resuelve en los casos de uso.</p>
 */
@RestController
@RequestMapping("/api/v1/portafolios/{portafolioId}")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.inversiones",
    name = "expuesta",
    havingValue = "true"
)
public class MovimientoController {

    private static final int PAGINA_POR_DEFECTO = 0;
    private static final int TAMANO_POR_DEFECTO = 20;

    private final RegistrarMovimientoUseCase registrarMovimiento;
    private final RevertirMovimientoUseCase revertirMovimiento;
    private final ListarMovimientosUseCase listarMovimientos;
    private final ConsultarMovimientoUseCase consultarMovimiento;
    private final ResumenPosicionUseCase resumenPosicion;
    private final ReconstruirPosicionUseCase reconstruirPosicion;
    private final MovimientoResponseAssembler assembler;

    public MovimientoController(
        RegistrarMovimientoUseCase registrarMovimiento,
        RevertirMovimientoUseCase revertirMovimiento,
        ListarMovimientosUseCase listarMovimientos,
        ConsultarMovimientoUseCase consultarMovimiento,
        ResumenPosicionUseCase resumenPosicion,
        ReconstruirPosicionUseCase reconstruirPosicion,
        MovimientoResponseAssembler assembler
    ) {
        this.registrarMovimiento = registrarMovimiento;
        this.revertirMovimiento = revertirMovimiento;
        this.listarMovimientos = listarMovimientos;
        this.consultarMovimiento = consultarMovimiento;
        this.resumenPosicion = resumenPosicion;
        this.reconstruirPosicion = reconstruirPosicion;
        this.assembler = assembler;
    }

    @GetMapping("/posiciones/{posicionId}/movimientos")
    public ResponseEntity<PaginaMovimientosResponse> listar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @RequestParam(name = "tipo", required = false) String tipo,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @RequestParam(name = "pagina", defaultValue = "" + PAGINA_POR_DEFECTO)
        int pagina,
        @RequestParam(name = "tamano", defaultValue = "" + TAMANO_POR_DEFECTO)
        int tamano,
        @RequestParam(name = "orden", defaultValue = "desc") String orden,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        PaginaMovimientosView resultado = listarMovimientos.execute(
            new ListarMovimientosQuery(
                usuario.id(),
                portafolioId,
                posicionId,
                tipo == null || tipo.isBlank()
                    ? null
                    : TipoMovimiento.desdeCodigo(tipo),
                desde,
                hasta,
                pagina,
                tamano,
                !"asc".equalsIgnoreCase(orden)));
        return ResponseEntity.ok(assembler.toResponse(resultado));
    }

    @PostMapping("/posiciones/{posicionId}/movimientos")
    public ResponseEntity<MovimientoResponse> registrar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @RequestHeader(name = "Idempotency-Key", required = false)
        String idempotencyKey,
        @Valid @RequestBody RegistrarMovimientoRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        MovimientoView creado = registrarMovimiento.execute(
            new RegistrarMovimientoCommand(
                usuario.id(),
                portafolioId,
                posicionId,
                TipoMovimiento.desdeCodigo(request.tipo()),
                request.fechaOperacion(),
                request.fechaLiquidacion(),
                request.cantidad(),
                request.precioUnitario(),
                request.comisiones(),
                request.impuestos(),
                request.tipoCambio(),
                request.costoTotalAjuste(),
                request.observaciones(),
                clave(request.claveIdempotencia(), idempotencyKey)));
        return ResponseEntity
            .created(URI.create(
                "/api/v1/portafolios/" + portafolioId
                    + "/movimientos/" + creado.id()))
            .body(assembler.toResponse(creado));
    }

    @GetMapping("/movimientos/{movimientoId}")
    public ResponseEntity<MovimientoResponse> consultar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("movimientoId") UUID movimientoId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        MovimientoView movimiento = consultarMovimiento.execute(
            new ConsultarMovimientoQuery(
                usuario.id(), portafolioId, movimientoId));
        return ResponseEntity.ok(assembler.toResponse(movimiento));
    }

    @PostMapping("/movimientos/{movimientoId}/reversion")
    public ResponseEntity<MovimientoResponse> revertir(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("movimientoId") UUID movimientoId,
        @RequestHeader(name = "Idempotency-Key", required = false)
        String idempotencyKey,
        @Valid @RequestBody RevertirMovimientoRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        MovimientoView reversion = revertirMovimiento.execute(
            new RevertirMovimientoCommand(
                usuario.id(),
                portafolioId,
                movimientoId,
                request.fechaOperacion(),
                request.observaciones(),
                clave(request.claveIdempotencia(), idempotencyKey)));
        return ResponseEntity
            .created(URI.create(
                "/api/v1/portafolios/" + portafolioId
                    + "/movimientos/" + reversion.id()))
            .body(assembler.toResponse(reversion));
    }

    @GetMapping("/posiciones/{posicionId}/resumen")
    public ResponseEntity<ResumenPosicionResponse> resumen(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        ResumenPosicionView resumen = resumenPosicion.execute(
            new ResumenPosicionQuery(usuario.id(), portafolioId, posicionId));
        return ResponseEntity.ok(assembler.toResponse(resumen));
    }

    @PostMapping("/posiciones/{posicionId}/reconstruccion")
    public ResponseEntity<ReconstruccionResponse> reconstruir(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @RequestBody(required = false) ReconstruirPosicionRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        ReconstruccionView reconstruccion = reconstruirPosicion.execute(
            new ReconstruirPosicionCommand(
                usuario.id(),
                portafolioId,
                posicionId,
                request != null && request.debeReparar()));
        return ResponseEntity.ok(assembler.toResponse(reconstruccion));
    }

    private static String clave(String delCuerpo, String cabecera) {
        String normalizada = TextosInversiones.opcional(delCuerpo);
        return normalizada != null
            ? normalizada
            : TextosInversiones.opcional(cabecera);
    }
}
