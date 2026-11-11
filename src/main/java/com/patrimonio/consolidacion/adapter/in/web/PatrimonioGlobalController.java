package com.patrimonio.consolidacion.adapter.in.web;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.CentroResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.DistribucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EventosResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.EvolucionResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.LiquidezResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.RendimientosResponse;
import com.patrimonio.consolidacion.adapter.in.web.PatrimonioResponse.ResumenResponse;
import com.patrimonio.consolidacion.application.port.in.ConsultarCentroPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarDistribucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarEventosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarEvolucionPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarLiquidezPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarRendimientosPatrimonioUseCase;
import com.patrimonio.consolidacion.application.port.in.ConsultarResumenPatrimonioUseCase;
import com.patrimonio.consolidacion.application.query.ConsultarPatrimonioQuery;
import com.patrimonio.consolidacion.domain.model.DimensionDistribucion;
import com.patrimonio.consolidacion.domain.model.PeriodoPatrimonial;
import com.patrimonio.shared.adapter.in.web.PerfilDeDesarrolloExplicito;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Centro de Patrimonio consolidado de todos los portafolios autorizados.
 *
 * <p>El servicio obtiene únicamente los portafolios donde el usuario tiene
 * membresía. Nunca recorre portafolios ajenos ni convierte monedas: cada
 * importe conserva su divisa y los porcentajes se calculan por moneda.</p>
 */
@RestController
@RequestMapping("/api/v1/patrimonio")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.consolidacion",
    name = "expuesta",
    havingValue = "true"
)
public class PatrimonioGlobalController {

    private final ConsultarCentroPatrimonioUseCase centro;
    private final ConsultarResumenPatrimonioUseCase resumen;
    private final ConsultarDistribucionPatrimonioUseCase distribucion;
    private final ConsultarEvolucionPatrimonioUseCase evolucion;
    private final ConsultarRendimientosPatrimonioUseCase rendimientos;
    private final ConsultarLiquidezPatrimonioUseCase liquidez;
    private final ConsultarEventosPatrimonioUseCase eventos;
    private final PatrimonioResponseAssembler assembler;

    public PatrimonioGlobalController(
        ConsultarCentroPatrimonioUseCase centro,
        ConsultarResumenPatrimonioUseCase resumen,
        ConsultarDistribucionPatrimonioUseCase distribucion,
        ConsultarEvolucionPatrimonioUseCase evolucion,
        ConsultarRendimientosPatrimonioUseCase rendimientos,
        ConsultarLiquidezPatrimonioUseCase liquidez,
        ConsultarEventosPatrimonioUseCase eventos,
        PatrimonioResponseAssembler assembler
    ) {
        this.centro = centro;
        this.resumen = resumen;
        this.distribucion = distribucion;
        this.evolucion = evolucion;
        this.rendimientos = rendimientos;
        this.liquidez = liquidez;
        this.eventos = eventos;
        this.assembler = assembler;
    }

    @GetMapping("/centro")
    public ResponseEntity<CentroResponse> centro(
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "periodo", required = false) String periodo,
        @RequestParam(name = "dimension", required = false) String dimension,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.centro(centro.centro(
            query(usuario, moneda, periodo, dimension, desde, hasta))));
    }

    @GetMapping("/resumen")
    public ResponseEntity<ResumenResponse> resumen(
        @RequestParam(name = "moneda", required = false) String moneda,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.resumen(resumen.resumen(
            ConsultarPatrimonioQuery.centro(
                usuario.id(),
                null,
                moneda,
                PeriodoPatrimonial.TODO))));
    }

    @GetMapping("/distribucion")
    public ResponseEntity<DistribucionResponse> distribucion(
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "dimension", required = false) String dimension,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.distribucion(distribucion.distribucion(
            new ConsultarPatrimonioQuery(
                usuario.id(),
                null,
                moneda,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.desdeCodigo(dimension),
                null,
                null))));
    }

    @GetMapping("/evolucion")
    public ResponseEntity<EvolucionResponse> evolucion(
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "periodo", required = false) String periodo,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.evolucion(evolucion.evolucion(
            query(usuario, moneda, periodo, null, desde, hasta))));
    }

    @GetMapping("/rendimientos")
    public ResponseEntity<RendimientosResponse> rendimientos(
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "periodo", required = false) String periodo,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.rendimientos(rendimientos.rendimientos(
            query(usuario, moneda, periodo, null, desde, hasta))));
    }

    @GetMapping("/liquidez")
    public ResponseEntity<LiquidezResponse> liquidez(
        @RequestParam(name = "moneda", required = false) String moneda,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.liquidez(liquidez.liquidez(
            ConsultarPatrimonioQuery.centro(
                usuario.id(),
                null,
                moneda,
                PeriodoPatrimonial.TODO))));
    }

    @GetMapping("/eventos")
    public ResponseEntity<EventosResponse> eventos(
        @RequestParam(name = "moneda", required = false) String moneda,
        @RequestParam(name = "desde", required = false) LocalDate desde,
        @RequestParam(name = "hasta", required = false) LocalDate hasta,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        return ResponseEntity.ok(assembler.eventos(eventos.eventos(
            new ConsultarPatrimonioQuery(
                usuario.id(),
                null,
                moneda,
                PeriodoPatrimonial.TODO,
                DimensionDistribucion.CLASE_ACTIVO,
                desde,
                hasta))));
    }

    private static ConsultarPatrimonioQuery query(
        UsuarioAutenticado usuario,
        String moneda,
        String periodo,
        String dimension,
        LocalDate desde,
        LocalDate hasta
    ) {
        return new ConsultarPatrimonioQuery(
            usuario.id(),
            null,
            moneda,
            PeriodoPatrimonial.desdeCodigo(periodo),
            DimensionDistribucion.desdeCodigo(dimension),
            desde,
            hasta);
    }
}
