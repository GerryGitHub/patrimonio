package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import com.patrimonio.cuentas.application.command.CrearInstitucionCommand;
import com.patrimonio.cuentas.application.port.in.ConsultarInstitucionUseCase;
import com.patrimonio.cuentas.application.port.in.CrearInstitucionUseCase;
import com.patrimonio.cuentas.application.port.in.ListarInstitucionesUseCase;
import com.patrimonio.cuentas.application.query.ListarInstitucionesQuery;
import com.patrimonio.cuentas.application.view.InstitucionView;
import com.patrimonio.shared.adapter.in.web.PerfilDeDesarrolloExplicito;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST del catálogo de instituciones financieras.
 *
 * <p>El catálogo es global y reutilizable: cualquier usuario autenticado puede
 * consultarlo y registrar instituciones. No se exige membresía sobre un
 * portafolio para operar sobre él.</p>
 */
@RestController
@RequestMapping("/api/v1/instituciones")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.cuentas",
    name = "expuesta",
    havingValue = "true"
)
public class InstitucionController {

    private final ListarInstitucionesUseCase listarInstituciones;
    private final ConsultarInstitucionUseCase consultarInstitucion;
    private final CrearInstitucionUseCase crearInstitucion;
    private final InstitucionResponseAssembler assembler;

    public InstitucionController(
        ListarInstitucionesUseCase listarInstituciones,
        ConsultarInstitucionUseCase consultarInstitucion,
        CrearInstitucionUseCase crearInstitucion,
        InstitucionResponseAssembler assembler
    ) {
        this.listarInstituciones = listarInstituciones;
        this.consultarInstitucion = consultarInstitucion;
        this.crearInstitucion = crearInstitucion;
        this.assembler = assembler;
    }

    @GetMapping
    public ResponseEntity<List<InstitucionResponse>> listar(
        @RequestParam(name = "tipo", required = false) TipoInstitucion tipo,
        @RequestParam(name = "activo", required = false) Boolean activo
    ) {
        List<InstitucionView> instituciones = listarInstituciones.execute(
            new ListarInstitucionesQuery(tipo, activo));

        return ResponseEntity.ok(
            instituciones.stream().map(assembler::toResponse).toList());
    }

    @GetMapping("/{institucionId}")
    public ResponseEntity<InstitucionResponse> consultar(
        @PathVariable("institucionId") UUID institucionId
    ) {
        return ResponseEntity.ok(
            assembler.toResponse(consultarInstitucion.execute(institucionId)));
    }

    @PostMapping
    public ResponseEntity<InstitucionResponse> crear(
        @Valid @RequestBody CrearInstitucionRequest request
    ) {
        InstitucionView creada = crearInstitucion.execute(
            new CrearInstitucionCommand(
                request.nombre(),
                request.tipo(),
                request.pais(),
                request.sitioWeb()));

        return ResponseEntity
            .created(URI.create("/api/v1/instituciones/" + creada.id()))
            .body(assembler.toResponse(creada));
    }
}
