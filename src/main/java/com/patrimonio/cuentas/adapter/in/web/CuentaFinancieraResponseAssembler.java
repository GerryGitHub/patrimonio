package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte vistas de aplicación en DTOs de respuesta REST.
 */
@Component
public class CuentaFinancieraResponseAssembler {

    public List<CuentaFinancieraResponse> toResponses(List<CuentaFinancieraView> cuentas) {
        return cuentas.stream().map(this::toResponse).toList();
    }

    private CuentaFinancieraResponse toResponse(CuentaFinancieraView cuenta) {
        return new CuentaFinancieraResponse(
            cuenta.id(),
            cuenta.nombre(),
            cuenta.numeroCuenta(),
            cuenta.institucionId(),
            cuenta.tipoCuentaId(),
            cuenta.monedaId(),
            cuenta.activo(),
            cuenta.fechaApertura(),
            cuenta.fechaCierre()
        );
    }
}
