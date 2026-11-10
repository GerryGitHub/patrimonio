package com.patrimonio.inversiones.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.inversiones.application.command.ActualizarInstrumentoCommand;
import com.patrimonio.inversiones.application.command.CambiarEstadoInstrumentoCommand;
import com.patrimonio.inversiones.application.command.CrearInstrumentoCommand;
import com.patrimonio.inversiones.application.port.in.ActualizarInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.in.CambiarEstadoInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.in.ConsultarInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.in.CrearInstrumentoUseCase;
import com.patrimonio.inversiones.application.port.in.ListarCatalogoInstrumentosUseCase;
import com.patrimonio.inversiones.application.port.in.ListarInstrumentosUseCase;
import com.patrimonio.inversiones.application.query.ConsultarInstrumentoQuery;
import com.patrimonio.inversiones.application.query.ListarInstrumentosQuery;
import com.patrimonio.inversiones.application.view.ClaseActivoView;
import com.patrimonio.inversiones.application.view.InstrumentoView;
import com.patrimonio.inversiones.application.view.TipoInstrumentoView;
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
 * Adaptador de entrada REST del catálogo de instrumentos.
 *
 * <p>El catálogo es global: cualquier usuario autenticado puede consultarlo y
 * administrarlo. El controlador solo se registra con
 * {@code patrimonio.api.inversiones.expuesta=true} y un perfil {@code local} o
 * {@code test} activo de forma explícita.</p>
 */
@RestController
@RequestMapping("/api/v1/instrumentos")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.inversiones",
    name = "expuesta",
    havingValue = "true"
)
public class InstrumentoController {

    private final ListarInstrumentosUseCase listarInstrumentos;
    private final ConsultarInstrumentoUseCase consultarInstrumento;
    private final CrearInstrumentoUseCase crearInstrumento;
    private final ActualizarInstrumentoUseCase actualizarInstrumento;
    private final CambiarEstadoInstrumentoUseCase cambiarEstadoInstrumento;
    private final ListarCatalogoInstrumentosUseCase listarCatalogo;
    private final InstrumentoResponseAssembler assembler;

    public InstrumentoController(
        ListarInstrumentosUseCase listarInstrumentos,
        ConsultarInstrumentoUseCase consultarInstrumento,
        CrearInstrumentoUseCase crearInstrumento,
        ActualizarInstrumentoUseCase actualizarInstrumento,
        CambiarEstadoInstrumentoUseCase cambiarEstadoInstrumento,
        ListarCatalogoInstrumentosUseCase listarCatalogo,
        InstrumentoResponseAssembler assembler
    ) {
        this.listarInstrumentos = listarInstrumentos;
        this.consultarInstrumento = consultarInstrumento;
        this.crearInstrumento = crearInstrumento;
        this.actualizarInstrumento = actualizarInstrumento;
        this.cambiarEstadoInstrumento = cambiarEstadoInstrumento;
        this.listarCatalogo = listarCatalogo;
        this.assembler = assembler;
    }

    @GetMapping
    public ResponseEntity<List<InstrumentoResponse>> listar(
        @RequestParam(name = "busqueda", required = false) String busqueda,
        @RequestParam(name = "claseActivoId", required = false) UUID claseActivoId,
        @RequestParam(name = "tipoInstrumentoId", required = false)
        UUID tipoInstrumentoId,
        @RequestParam(name = "activo", required = false) Boolean activo
    ) {
        List<InstrumentoView> instrumentos = listarInstrumentos.execute(
            new ListarInstrumentosQuery(
                busqueda, claseActivoId, tipoInstrumentoId, activo));
        return ResponseEntity.ok(assembler.toResponses(instrumentos));
    }

    @GetMapping("/categorias")
    public ResponseEntity<List<ClaseActivoView>> listarCategorias() {
        return ResponseEntity.ok(listarCatalogo.listarClases());
    }

    @GetMapping("/tipos")
    public ResponseEntity<List<TipoInstrumentoView>> listarTipos(
        @RequestParam(name = "claseActivoId", required = false) UUID claseActivoId
    ) {
        return ResponseEntity.ok(listarCatalogo.listarTipos(claseActivoId));
    }

    @GetMapping("/{instrumentoId}")
    public ResponseEntity<InstrumentoResponse> consultar(
        @PathVariable("instrumentoId") UUID instrumentoId
    ) {
        InstrumentoView instrumento = consultarInstrumento.execute(
            new ConsultarInstrumentoQuery(instrumentoId));
        return ResponseEntity.ok(assembler.toResponse(instrumento));
    }

    @PostMapping
    public ResponseEntity<InstrumentoResponse> crear(
        @Valid @RequestBody CrearInstrumentoRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        InstrumentoView creado = crearInstrumento.execute(
            new CrearInstrumentoCommand(
                usuario.id(),
                request.tipoInstrumentoId(),
                request.nombre(),
                request.simbolo(),
                request.emisor(),
                request.mercado(),
                request.monedaId(),
                request.identificadorExterno(),
                request.isin(),
                request.descripcion()));

        return ResponseEntity
            .created(URI.create("/api/v1/instrumentos/" + creado.id()))
            .body(assembler.toResponse(creado));
    }

    @PutMapping("/{instrumentoId}")
    public ResponseEntity<InstrumentoResponse> actualizar(
        @PathVariable("instrumentoId") UUID instrumentoId,
        @Valid @RequestBody ActualizarInstrumentoRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        InstrumentoView actualizado = actualizarInstrumento.execute(
            new ActualizarInstrumentoCommand(
                usuario.id(),
                instrumentoId,
                request.tipoInstrumentoId(),
                request.nombre(),
                request.simbolo(),
                request.emisor(),
                request.mercado(),
                request.monedaId(),
                request.identificadorExterno(),
                request.isin(),
                request.descripcion()));

        return ResponseEntity.ok(assembler.toResponse(actualizado));
    }

    @PatchMapping("/{instrumentoId}/estado")
    public ResponseEntity<InstrumentoResponse> cambiarEstado(
        @PathVariable("instrumentoId") UUID instrumentoId,
        @Valid @RequestBody CambiarEstadoInstrumentoRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        InstrumentoView actualizado = cambiarEstadoInstrumento.execute(
            new CambiarEstadoInstrumentoCommand(
                usuario.id(), instrumentoId, request.activo()));
        return ResponseEntity.ok(assembler.toResponse(actualizado));
    }
}
