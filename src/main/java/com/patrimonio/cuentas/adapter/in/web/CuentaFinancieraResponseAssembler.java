package com.patrimonio.cuentas.adapter.in.web;

import com.patrimonio.cuentas.application.view.CuentaFinancieraView;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Convierte vistas de aplicación en DTOs de respuesta REST.
 *
 * <p>El número de cuenta se enmascara aquí, en el borde HTTP. El dominio y la
 * persistencia conservan el valor completo para uso interno.</p>
 */
@Component
public class CuentaFinancieraResponseAssembler {

    public List<CuentaFinancieraResponse> toResponses(List<CuentaFinancieraView> cuentas) {
        return cuentas.stream().map(this::toResponse).toList();
    }

    public CuentaFinancieraResponse toResponse(CuentaFinancieraView cuenta) {
        return new CuentaFinancieraResponse(
            cuenta.id(),
            cuenta.nombre(),
            EnmascaradorNumeroCuenta.enmascarar(cuenta.numeroCuenta()),
            cuenta.institucionId(),
            cuenta.tipoCuentaId(),
            cuenta.monedaId(),
            cuenta.activo(),
            cuenta.fechaApertura(),
            cuenta.fechaCierre(),
            cuenta.descripcion()
        );
    }
}
