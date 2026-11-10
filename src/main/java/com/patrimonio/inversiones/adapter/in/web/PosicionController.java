package com.patrimonio.inversiones.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.inversiones.application.command.ActualizarPosicionCommand;
import com.patrimonio.inversiones.application.command.CambiarEstadoPosicionCommand;
import com.patrimonio.inversiones.application.command.CrearPosicionCommand;
import com.patrimonio.inversiones.application.port.in.ActualizarPosicionUseCase;
import com.patrimonio.inversiones.application.port.in.CambiarEstadoPosicionUseCase;
import com.patrimonio.inversiones.application.port.in.ConsultarPosicionUseCase;
import com.patrimonio.inversiones.application.port.in.CrearPosicionUseCase;
import com.patrimonio.inversiones.application.port.in.ListarPosicionesUseCase;
import com.patrimonio.inversiones.application.port.in.ResumenPosicionesUseCase;
import com.patrimonio.inversiones.application.query.ConsultarPosicionQuery;
import com.patrimonio.inversiones.application.query.ListarPosicionesQuery;
import com.patrimonio.inversiones.application.query.ResumenPosicionesQuery;
import com.patrimonio.inversiones.application.view.PosicionView;
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
 * Adaptador de entrada REST de las posiciones de un portafolio.
 *
 * <p>La cuenta y el instrumento se validan dentro de los casos de uso. El
 * usuario autenticado se toma del contexto de seguridad y la autorización por
 * membresía se resuelve en la capa de aplicación.</p>
 */
@RestController
@RequestMapping("/api/v1/portafolios/{portafolioId}/posiciones")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.inversiones",
    name = "expuesta",
    havingValue = "true"
)
public class PosicionController {

    private final ListarPosicionesUseCase listarPosiciones;
    private final ConsultarPosicionUseCase consultarPosicion;
    private final CrearPosicionUseCase crearPosicion;
    private final ActualizarPosicionUseCase actualizarPosicion;
    private final CambiarEstadoPosicionUseCase cambiarEstadoPosicion;
    private final ResumenPosicionesUseCase resumenPosiciones;
    private final PosicionResponseAssembler assembler;

    public PosicionController(
        ListarPosicionesUseCase listarPosiciones,
        ConsultarPosicionUseCase consultarPosicion,
        CrearPosicionUseCase crearPosicion,
        ActualizarPosicionUseCase actualizarPosicion,
        CambiarEstadoPosicionUseCase cambiarEstadoPosicion,
        ResumenPosicionesUseCase resumenPosiciones,
        PosicionResponseAssembler assembler
    ) {
        this.listarPosiciones = listarPosiciones;
        this.consultarPosicion = consultarPosicion;
        this.crearPosicion = crearPosicion;
        this.actualizarPosicion = actualizarPosicion;
        this.cambiarEstadoPosicion = cambiarEstadoPosicion;
        this.resumenPosiciones = resumenPosiciones;
        this.assembler = assembler;
    }

    @GetMapping
    public ResponseEntity<List<PosicionResponse>> listar(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "cuentaId", required = false) UUID cuentaId,
        @RequestParam(name = "instrumentoId", required = false) UUID instrumentoId,
        @RequestParam(name = "claseActivoId", required = false) UUID claseActivoId,
        @RequestParam(name = "activo", required = false) Boolean activo,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<PosicionView> posiciones = listarPosiciones.execute(
            new ListarPosicionesQuery(
                usuario.id(),
                portafolioId,
                cuentaId,
                instrumentoId,
                claseActivoId,
                activo));
        return ResponseEntity.ok(assembler.toResponses(posiciones));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenPosicionesResponse> resumen(
        @PathVariable("portafolioId") UUID portafolioId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.toResponse(
            resumenPosiciones.execute(
                new ResumenPosicionesQuery(usuario.id(), portafolioId))));
    }

    @GetMapping("/{posicionId}")
    public ResponseEntity<PosicionResponse> consultar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        PosicionView posicion = consultarPosicion.execute(
            new ConsultarPosicionQuery(usuario.id(), portafolioId, posicionId));
        return ResponseEntity.ok(assembler.toResponse(posicion));
    }

    @PostMapping
    public ResponseEntity<PosicionResponse> crear(
        @PathVariable("portafolioId") UUID portafolioId,
        @Valid @RequestBody CrearPosicionRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        PosicionView creada = crearPosicion.execute(
            new CrearPosicionCommand(
                usuario.id(),
                portafolioId,
                request.cuentaId(),
                request.instrumentoId(),
                request.cantidad(),
                request.costoUnitario(),
                request.fechaApertura(),
                request.observaciones()));

        return ResponseEntity
            .created(URI.create(
                "/api/v1/portafolios/" + portafolioId
                    + "/posiciones/" + creada.id()))
            .body(assembler.toResponse(creada));
    }

    @PutMapping("/{posicionId}")
    public ResponseEntity<PosicionResponse> actualizar(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @Valid @RequestBody ActualizarPosicionRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        PosicionView actualizada = actualizarPosicion.execute(
            new ActualizarPosicionCommand(
                usuario.id(),
                portafolioId,
                posicionId,
                request.cantidad(),
                request.costoUnitario(),
                request.fechaApertura(),
                request.observaciones()));

        return ResponseEntity.ok(assembler.toResponse(actualizada));
    }

    @PatchMapping("/{posicionId}/estado")
    public ResponseEntity<PosicionResponse> cambiarEstado(
        @PathVariable("portafolioId") UUID portafolioId,
        @PathVariable("posicionId") UUID posicionId,
        @Valid @RequestBody CambiarEstadoPosicionRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        PosicionView actualizada = cambiarEstadoPosicion.execute(
            new CambiarEstadoPosicionCommand(
                usuario.id(), portafolioId, posicionId, request.activo()));
        return ResponseEntity.ok(assembler.toResponse(actualizada));
    }
}
