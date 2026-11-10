package com.patrimonio.inversiones.application.support;

import com.patrimonio.cuentas.api.dto.CuentaResumen;
import com.patrimonio.inversiones.domain.error.PosicionIncompatibleException;
import com.patrimonio.inversiones.domain.error.PosicionInvalidaException;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import java.text.Normalizer;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * Reglas conservadoras de compatibilidad entre una cuenta y un instrumento.
 *
 * <p>Las reglas usan el nombre del tipo de cuenta porque el catálogo de tipos
 * de cuenta no tiene una clasificación funcional adicional. Se documentan
 * aquí y se pueden reemplazar por una tabla de compatibilidad cuando el
 * catálogo la incorpore:</p>
 *
 * <ul>
 *   <li>Cuenta a plazo: no admite posiciones bursátiles; corresponde a un
 *       contrato de inversión de la siguiente entrega.</li>
 *   <li>Cuenta de exchange o cripto: solo admite activos digitales.</li>
 *   <li>Cuenta de corretaje, casa de bolsa o inversión: admite renta variable
 *       y renta fija; no admite activos digitales.</li>
 *   <li>Cuenta corriente, de ahorro u otra: se permite cualquier clase para no
 *       bloquear casos legítimos mientras no exista una clasificación más
 *       precisa.</li>
 * </ul>
 */
@Component
public class ValidadorCompatibilidadCuentaInstrumento {

    public void exigirCuentaActiva(CuentaResumen cuenta) {
        if (!cuenta.activo()) {
            throw new PosicionInvalidaException(
                "La cuenta indicada está inactiva y no admite posiciones nuevas.");
        }
    }

    public void validar(CuentaResumen cuenta, ClaseActivo clase) {
        String tipoCuenta = normalizar(cuenta.tipoCuentaNombre());
        String claseActivo = normalizar(clase.nombre());

        if (tipoCuenta.contains("plazo")) {
            throw new PosicionIncompatibleException(
                "Las cuentas a plazo corresponden a contratos de inversión y no "
                    + "admiten posiciones bursátiles en esta entrega.");
        }

        if (tipoCuenta.contains("exchange") || tipoCuenta.contains("cripto")) {
            if (!claseActivo.contains("activos digitales")) {
                throw new PosicionIncompatibleException(
                    "Una cuenta de exchange solo admite instrumentos de activos digitales.");
            }
            return;
        }

        if (tipoCuenta.contains("corretaje")
            || tipoCuenta.contains("bolsa")
            || tipoCuenta.contains("inversion")) {
            if (!claseActivo.contains("renta variable")
                && !claseActivo.contains("renta fija")) {
                throw new PosicionIncompatibleException(
                    "Una cuenta de corretaje solo admite instrumentos de renta "
                        + "variable o renta fija.");
            }
        }
    }

    private static String normalizar(String valor) {
        if (valor == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(valor, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "");
        return sinAcentos.strip().toLowerCase(Locale.ROOT);
    }
}
