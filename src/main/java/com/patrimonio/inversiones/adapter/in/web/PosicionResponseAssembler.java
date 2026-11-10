package com.patrimonio.inversiones.adapter.in.web;

import com.patrimonio.inversiones.application.view.PosicionView;
import com.patrimonio.inversiones.application.view.ResumenPosicionesView;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte vistas de posiciones y agregados en DTOs de respuesta REST.
 *
 * <p>Los importes se serializan con {@link BigDecimal#toPlainString()} para no
 * perder precisión ni introducir notación científica.</p>
 */
@Component
public class PosicionResponseAssembler {

    public PosicionResponse toResponse(PosicionView vista) {
        return new PosicionResponse(
            vista.id(),
            vista.cuentaId(),
            vista.cuentaNombre(),
            vista.instrumentoId(),
            vista.instrumentoNombre(),
            vista.instrumentoSimbolo(),
            vista.tipoInstrumentoId(),
            vista.tipoInstrumentoNombre(),
            vista.claseActivoId(),
            vista.claseActivoNombre(),
            texto(vista.cantidad()),
            texto(vista.costoUnitario()),
            texto(vista.costoTotal()),
            vista.monedaId(),
            vista.monedaCodigo(),
            vista.monedaNombre(),
            vista.monedaSimbolo(),
            vista.monedaDecimales(),
            vista.observaciones(),
            vista.activo(),
            vista.fechaApertura(),
            vista.fechaCreacion(),
            vista.fechaActualizacion());
    }

    public List<PosicionResponse> toResponses(List<PosicionView> vistas) {
        return vistas.stream().map(this::toResponse).toList();
    }

    public ResumenPosicionesResponse toResponse(ResumenPosicionesView vista) {
        return new ResumenPosicionesResponse(
            vista.totalPosiciones(),
            vista.posicionesActivas(),
            vista.instrumentosDistintos(),
            vista.costoPorMoneda().stream()
                .map(this::toResponse)
                .toList(),
            vista.distribucionPorCategoria().stream()
                .map(this::toResponse)
                .toList());
    }

    private ResumenPosicionesResponse.CostoPorMonedaResponse toResponse(
        ResumenPosicionesView.CostoPorMonedaView vista
    ) {
        return new ResumenPosicionesResponse.CostoPorMonedaResponse(
            vista.monedaId(),
            vista.codigo(),
            vista.nombre(),
            vista.simbolo(),
            vista.decimales(),
            texto(vista.costo()));
    }

    private ResumenPosicionesResponse.DistribucionCategoriaResponse toResponse(
        ResumenPosicionesView.DistribucionCategoriaView vista
    ) {
        return new ResumenPosicionesResponse.DistribucionCategoriaResponse(
            vista.claseActivoId(),
            vista.claseActivoNombre(),
            vista.posiciones(),
            vista.instrumentos(),
            vista.costoPorMoneda().stream()
                .map(this::toResponse)
                .toList());
    }

    private static String texto(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
