package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.application.port.in.ListarTiposCuentaUseCase;
import com.patrimonio.cuentas.application.view.TipoCuentaView;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Conditional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Adaptador de entrada REST del catálogo de tipos de cuenta.
 */
@RestController
@RequestMapping("/api/v1/tipos-cuenta")
@Conditional(PerfilDeDesarrolloExplicito.class)
@ConditionalOnProperty(
    prefix = "patrimonio.api.cuentas",
    name = "expuesta",
    havingValue = "true"
)
public class TipoCuentaController {

    private final ListarTiposCuentaUseCase listarTiposCuenta;

    public TipoCuentaController(ListarTiposCuentaUseCase listarTiposCuenta) {
        this.listarTiposCuenta = listarTiposCuenta;
    }

    @GetMapping
    public ResponseEntity<List<TipoCuentaResponse>> listar() {
        List<TipoCuentaView> tipos = listarTiposCuenta.execute();

        return ResponseEntity.ok(
            tipos.stream()
                .map(tipo -> new TipoCuentaResponse(
                    tipo.id(),
                    tipo.nombre(),
                    tipo.descripcion(),
                    tipo.activo()))
                .toList());
    }
}
