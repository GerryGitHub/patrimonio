package com.patrimonio.consolidacion.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.CentroResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.DistribucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EventosResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EvolucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.LiquidezResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.RendimientosResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.ResumenResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.SnapshotResponse;
import com.patrimonio.consolidacion.application.command.CrearSnapshotPatrimonioCommand;
import com.patrimonio.consolidacion.application.port.in.ConsultarCentroPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarDistribucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarEventosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarEvolucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarLiquidezPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarRendimientosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarResumenPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.CrearSnapshotPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ListarSnapshotsPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.application.query.ConsultarSnapshotsQuery;
import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.consolidacion.domain.model.PeriodoPatrimonial;
import com.patrimonio.shared.adapter.in.web.PerfilDeDesarrolloExplicito;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST del Centro de Patrimonio de un portafolio.
 *
 * <p>Todos los endpoints exigen autenticación y autorización por membresía,
 * resuelta en la capa de aplicación. Los importes viajan como cadena con
 * escala fija y los indicadores sin datos se devuelven como no disponibles,
 * nunca como cero.</p>
 */
@RestController
@RequestMapping("/api/v1/portafolios/{portafolioId}/patrimonio")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.consolidacion",
    name = "expuesta",
    havingValue = "true"
)
public class PatrimonioController {

    private final ConsultarCentroPatrimonioUseCase centro;
    private final ConsultarResumenPatrimonioUseCase resumen;
    private final ConsultarDistribucionPatrimonioUseCase distribucion;
    private final ConsultarEvolucionPatrimonioUseCase evolucion;
    private final ConsultarRendimientosPatrimonioUseCase rendimientos;
    private final ConsultarLiquidezPatrimonioUseCase liquidez;
    private final ConsultarEventosPatrimonioUseCase eventos;
    private final CrearSnapshotPatrimonioUseCase crearSnapshot;
    private final ListarSnapshotsPatrimonioUseCase listarSnapshots;
    private final PatrimonioResponseAssembler assembler;

    public PatrimonioController(
        ConsultarCentroPatrimonioUseCase centro,
        ConsultarResumenPatrimonioUseCase resumen,
        ConsultarDistribucionPatrimonioUseCase distribucion,
        ConsultarEvolucionPatrimonioUseCase evolucion,
        ConsultarRendimientosPatrimonioUseCase rendimientos,
        ConsultarLiquidezPatrimonioUseCase liquidez,
        ConsultarEventosPatrimonioUseCase eventos,
        CrearSnapshotPatrimonioUseCase crearSnapshot,
        ListarSnapshotsPatrimonioUseCase listarSnapshots,
        PatrimonioResponseAssembler assembler
    ) {
        this.centro = centro;
        this.resumen = resumen;
        this.distribucion = distribucion;
        this.evolucion = evolucion;
        this.rendimientos = rendimientos;
        this.liquidez = liquidez;
        this.eventos = eventos;
        this.crearSnapshot = crearSnapshot;
        this.listarSnapshots = listarSnapshots;
        this.assembler = assembler;
    }

    @GetMapping("/centro")
    public ResponseEntity<CentroResponse> centro(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "periodo", required = false) String periodo,
        @RequestParam(name = "dimension", required = false) String dimension,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.centro(centro.execute(
            query(usuario, portafolioId, moneda, periodo, dimension, desde, hasta))));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenResponse> resumen(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.resumen(resumen.execute(
            ConsultarPatrimonioQuery.centro(
                usuario.id(),
                portafolioId,
                moneda,
                PeriodoPatrimonial.TODO))));
    }

    @GetMapping("/distribucion")
    public ResponseEntity<DistribucionResponse> distribucion(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "dimension", required = false) String dimension,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.distribucion(distribucion.execute(
            new ConsultarPatrimonioQuery(
                usuario.id(),
                portafolioId,
                moneda,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.desdeCodigo(dimension),
                null,
                null))));
    }

    @GetMapping("/evolucion")
    public ResponseEntity<EvolucionResponse> evolucion(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "periodo", required = false) String periodo,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.evolucion(evolucion.execute(
            query(usuario, portafolioId, moneda, periodo, null, desde, hasta))));
    }

    @GetMapping("/rendimientos")
    public ResponseEntity<RendimientosResponse> rendimientos(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "periodo", required = false) String periodo,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.rendimientos(rendimientos.execute(
            query(usuario, portafolioId, moneda, periodo, null, desde, hasta))));
    }

    @GetMapping("/liquidez")
    public ResponseEntity<LiquidezResponse> liquidez(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.liquidez(liquidez.execute(
            ConsultarPatrimonioQuery.centro(
                usuario.id(),
                portafolioId,
                moneda,
                PeriodoPatrimonial.TODO))));
    }

    @GetMapping("/eventos")
    public ResponseEntity<EventosResponse> eventos(
        @PathVariable("portafolioId") UUID portafolioId,
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.eventos(eventos.execute(
            new ConsultarPatrimonioQuery(
                usuario.id(),
                portafolioId,
                moneda,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.CLASE_ACTIVO,
                desde,
                hasta))));
    }

    @GetMapping("/snapshots")
    public ResponseEntity<List<SnapshotResponse>> snapshots(
        @PathVariable("portafolioId") UUID portafolioId,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(listarSnapshots
            .execute(new ConsultarSnapshotsQuery(usuario.id(), portafolioId))
            .stream()
            .map(assembler::snapshot)
            .toList());
    }

    @PostMapping("/snapshots")
    public ResponseEntity<List<SnapshotResponse>> crearSnapshot(
        @PathVariable("portafolioId") UUID portafolioId,
        @Valid @RequestBody(required = false) CrearSnapshotRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<SnapshotResponse> creados = crearSnapshot
            .execute(new CrearSnapshotPatrimonioCommand(
                usuario.id(),
                portafolioId,
                request == null ? null : request.fecha()))
            .stream()
            .map(assembler::snapshot)
            .toList();
        return ResponseEntity
            .created(URI.create(
                "/api/v1/portafolios/" + portafolioId
                    + "/patrimonio/snapshots"))
            .body(creados);
    }

    private static ConsultarPatrimonioQuery query(
        UsuarioAutenticado usuario,
        UUID portafolioId,
        String moneda,
        String periodo,
        String dimension,
        LocalDate desde,
        LocalDate hasta
    ) {
        return new ConsultarPatrimonioQuery(
            usuario.id(),
            portafolioId,
            moneda,
            PeriodoPatrimonial.desdeCodigo(periodo),
            DimensionDistribucion.desdeCodigo(dimension),
            desde,
            hasta);
    }
}
