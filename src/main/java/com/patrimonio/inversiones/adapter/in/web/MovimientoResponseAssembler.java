package com.patrimonio.inversiones.adapter.in.web;

import com.patrimonio.inversiones.application.view.MovimientoView;
import com.patrimonio.inversiones.application.view.PaginaMovimientosView;
import com.patrimonio.inversiones.application.view.ReconstruccionView;
import com.patrimonio.inversiones.application.view.ResumenPosicionView;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte vistas de movimientos y saldos en DTOs REST.
 */
@Component
public class MovimientoResponseAssembler {

    public MovimientoResponse toResponse(MovimientoView vista) {
        return new MovimientoResponse(
            vista.id(),
            vista.portafolioId(),
            vista.cuentaId(),
            vista.cuentaNombre(),
            vista.posicionId(),
            vista.instrumentoId(),
            vista.instrumentoNombre(),
            vista.instrumentoSimbolo(),
            vista.tipo().codigo(),
            vista.tipoNombre(),
            vista.fechaOperacion(),
            vista.fechaLiquidacion(),
            texto(vista.cantidad()),
            texto(vista.precioUnitario()),
            texto(vista.importeBruto()),
            texto(vista.comisiones()),
            texto(vista.impuestos()),
            vista.monedaId(),
            vista.monedaCodigo(),
            vista.monedaNombre(),
            vista.monedaSimbolo(),
            vista.monedaDecimales(),
            texto(vista.tipoCambio()),
            texto(vista.importeNeto()),
            texto(vista.costoAplicado()),
            texto(vista.resultadoRealizado()),
            texto(vista.cantidadResultante()),
            texto(vista.costoResultante()),
            vista.estado(),
            vista.usuarioId(),
            vista.movimientoOrigenId(),
            vista.revertido(),
            vista.observaciones(),
            vista.fechaCreacion());
    }

    public List<MovimientoResponse> toResponses(List<MovimientoView> vistas) {
        return vistas.stream().map(this::toResponse).toList();
    }

    public PaginaMovimientosResponse toResponse(PaginaMovimientosView vista) {
        return new PaginaMovimientosResponse(
            toResponses(vista.items()),
            vista.pagina(),
            vista.tamano(),
            vista.total(),
            vista.totalPaginas());
    }

    public ResumenPosicionResponse toResponse(ResumenPosicionView vista) {
        return new ResumenPosicionResponse(
            vista.posicionId(),
            vista.portafolioId(),
            vista.cuentaId(),
            vista.instrumentoId(),
            vista.instrumentoNombre(),
            vista.instrumentoSymbol(),
            texto(vista.cantidad()),
            texto(vista.costoTotal()),
            texto(vista.costoUnitario()),
            texto(vista.resultadoRealizado()),
            vista.monedaId(),
            vista.monedaCodigo(),
            vista.monedaNombre(),
            vista.monedaSimbolo(),
            vista.monedaDecimales(),
            vista.fechaUltimoMovimiento(),
            vista.movimientosConfirmados(),
            vista.activo());
    }

    public ReconstruccionResponse toResponse(ReconstruccionView vista) {
        return new ReconstruccionResponse(
            vista.posicionId(),
            vista.coincide(),
            vista.reparada(),
            texto(vista.cantidadCalculada()),
            texto(vista.costoCalculado()),
            texto(vista.resultadoCalculado()),
            texto(vista.cantidadMaterializada()),
            texto(vista.costoMaterializado()),
            texto(vista.resultadoMaterializado()),
            vista.diferencias());
    }

    private static String texto(BigDecimal valor) {
        return valor == null ? null : valor.toPlainString();
    }
}
