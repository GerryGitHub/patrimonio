package com.patrimonio.inversiones.adapter.in.web;

/**
 * Opciones de la reconstrucción de una posición.
 *
 * <p>El cuerpo es opcional: si se omite, {@code reparar} vale {@code false} y
 * la operación solo informa las diferencias. Nunca modifica movimientos.</p>
 */
public record ReconstruirPosicionRequest(Boolean reparar) {

    public boolean debeReparar() {
        return Boolean.TRUE.equals(reparar);
    }
}
