package com.patrimonio.inversiones.domain.model;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import java.util.Locale;

/**
 * Tipos de movimiento que afectan la tenencia de una posición.
 *
 * <p>Los dividendos, intereses, depósitos y retiros de efectivo no forman parte
 * de este catálogo: pertenecen a sus propios módulos. {@code REVERSION} no se
 * acepta en el alta directa; solo lo genera el caso de uso de reversión.</p>
 */
public enum TipoMovimiento {

    APERTURA("APERTURA", "Apertura", true, true),
    COMPRA("COMPRA", "Compra", true, true),
    VENTA("VENTA", "Venta", false, true),
    AJUSTE_POSITIVO("AJUSTE_POSITIVO", "Ajuste positivo", true, true),
    AJUSTE_NEGATIVO("AJUSTE_NEGATIVO", "Ajuste negativo", false, true),
    REVERSION("REVERSION", "Reversión", false, false);

    private final String codigo;
    private final String etiqueta;
    private final boolean incrementaCantidad;
    private final boolean operable;

    TipoMovimiento(
        String codigo,
        String etiqueta,
        boolean incrementaCantidad,
        boolean operable
    ) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
        this.incrementaCantidad = incrementaCantidad;
        this.operable = operable;
    }

    public String codigo() {
        return codigo;
    }

    /**
     * Nombre legible para la interfaz. El código estable sigue siendo
     * {@link #codigo()}.
     */
    public String etiqueta() {
        return etiqueta;
    }

    public boolean incrementaCantidad() {
        return incrementaCantidad;
    }

    /**
     * Indica si el tipo puede solicitarse directamente en el alta de un
     * movimiento. La reversión tiene su propio caso de uso.
     */
    public boolean operable() {
        return operable;
    }

    public boolean esAjuste() {
        return this == AJUSTE_POSITIVO || this == AJUSTE_NEGATIVO;
    }

    public static TipoMovimiento desdeCodigo(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new MovimientoInvalidoException(
                "El tipo de movimiento es obligatorio.");
        }
        String normalizado = valor.strip().toUpperCase(Locale.ROOT);
        for (TipoMovimiento tipo : values()) {
            if (tipo.codigo.equals(normalizado)) {
                return tipo;
            }
        }
        throw new MovimientoInvalidoException(
            "El tipo de movimiento '" + valor + "' no existe.");
    }
}
