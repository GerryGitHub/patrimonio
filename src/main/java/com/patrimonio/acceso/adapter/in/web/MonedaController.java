package com.patrimonio.acceso.adapter.in.web;

import com.patrimonio.acceso.application.port.in.ListarMonedasUseCase;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada para consultar el catálogo de monedas activas.
 *
 * <p>No crea ni modifica monedas: el catálogo pertenece a la base de datos y
 * se reutiliza desde el formulario de portafolios.</p>
 */
@RestController
@RequestMapping("/api/v1/monedas")
public class MonedaController {

    private final ListarMonedasUseCase listarMonedas;

    public MonedaController(ListarMonedasUseCase listarMonedas) {
        this.listarMonedas = listarMonedas;
    }

    @GetMapping
    public ResponseEntity<List<MonedaResponse>> listar() {
        return ResponseEntity.ok(
            listarMonedas.execute().stream()
                .map(MonedaResponse::from)
                .toList());
    }
}
