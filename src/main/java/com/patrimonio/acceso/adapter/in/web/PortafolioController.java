package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.port.in.ListarPortafoliosAutorizadosUseCase;
import com.patrimonio.acceso.application.query.ListarPortafoliosAutorizadosQuery;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada para listar los portafolios autorizados del usuario
 * autenticado.
 */
@RestController
@RequestMapping("/api/v1/portafolios")
public class PortafolioController {

    private final ListarPortafoliosAutorizadosUseCase listarPortafolios;

    public PortafolioController(
        ListarPortafoliosAutorizadosUseCase listarPortafolios
    ) {
        this.listarPortafolios = listarPortafolios;
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
}
