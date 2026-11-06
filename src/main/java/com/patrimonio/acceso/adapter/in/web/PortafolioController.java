package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.CrearPortafolioCommand;
import com.patrimonio.acceso.application.port.in.CrearPortafolioUseCase;
import com.patrimonio.acceso.application.port.in.ListarPortafoliosAutorizadosUseCase;
import com.patrimonio.acceso.application.query.ListarPortafoliosAutorizadosQuery;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada para listar y crear los portafolios del usuario
 * autenticado.
 *
 * <p>El usuario se toma siempre de la sesión; nunca del cuerpo de la
 * petición. La creación devuelve {@code 201 Created} con la representación del
 * portafolio y la cabecera {@code Location}.</p>
 */
@RestController
@RequestMapping("/api/v1/portafolios")
public class PortafolioController {

    private final ListarPortafoliosAutorizadosUseCase listarPortafolios;
    private final CrearPortafolioUseCase crearPortafolio;

    public PortafolioController(
        ListarPortafoliosAutorizadosUseCase listarPortafolios,
        CrearPortafolioUseCase crearPortafolio
    ) {
        this.listarPortafolios = listarPortafolios;
        this.crearPortafolio = crearPortafolio;
    }

    @GetMapping
    public ResponseEntity<List<PortafolioResponse>> listar(
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        List<PortafolioResumen> portafolios = listarPortafolios.execute(
            new ListarPortafoliosAutorizadosQuery(usuario.id()));

        return ResponseEntity.ok(
            portafolios.stream().map(PortafolioResponse::from).toList());
    }

    @PostMapping
    public ResponseEntity<PortafolioResponse> crear(
        @Valid @RequestBody CrearPortafolioRequest request,
        @AuthenticationPrincipal UsuarioAutenticado usuario
    ) {
        PortafolioResumen creado = crearPortafolio.execute(
            new CrearPortafolioCommand(
                usuario.id(),
                request.nombre(),
                request.descripcion(),
                request.monedaBaseId()));

        return ResponseEntity
            .created(URI.create("/api/v1/portafolios/" + creado.id()))
            .body(PortafolioResponse.from(creado));
    }
}
