package com.patrimonio.cuentas.application.support;

import com.patrimonio.cuentas.application.port.out.CuentaFinancieraRepository;
import com.patrimonio.cuentas.domain.error.CuentaDuplicadaException;
import com.patrimonio.cuentas.domain.model.CuentaFinanciera;
import com.patrimonio.cuentas.domain.model.NumeroCuenta;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resuelve y valida la referencia bancaria de una cuenta.
 *
 * <p>La referencia se normaliza recortando espacios exteriores; los espacios
 * interiores pueden ser significativos y se conservan. En la edición, si no
 * llega una referencia nueva se mantiene la existente, porque la API solo
 * expone la versión enmascarada. La comprobación de duplicidad excluye la
 * propia cuenta únicamente cuando se está editando.</p>
 */
@Component
public class ValidadorReferenciaCuenta {

    private final CuentaFinancieraRepository cuentas;

    public ValidadorReferenciaCuenta(CuentaFinancieraRepository cuentas) {
        this.cuentas = cuentas;
    }

    public String resolver(
        String referenciaRecibida,
        UUID institucionId,
        CuentaFinanciera existente
    ) {
        String numero = Textos.referencia(referenciaRecibida);
        if (numero == null && existente != null) {
            numero = existente.numeroCuenta()
                .map(NumeroCuenta::valor)
                .orElse(null);
        }

        UUID cuentaExcluidaId = existente == null ? null : existente.id();
        if (numero != null && cuentas.existeNumeroEnInstitucion(
            institucionId, numero, cuentaExcluidaId)) {
            throw new CuentaDuplicadaException(
                "Ya existe una cuenta con ese número en la institución.");
        }
        return numero;
    }
}
