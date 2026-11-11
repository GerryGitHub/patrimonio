package com.patrimonio.consolidacion.adapter.in.web;

import java.time.LocalDate;

/**
 * Solicitud de captura manual de un snapshot patrimonial.
 *
 * <p>La fecha es opcional. Si se omite, la captura usa la fecha actual en UTC.
 * No se permiten fechas pasadas porque el snapshot no reconstruye información
 * histórica: fotografía el estado disponible en el momento de la captura.</p>
 */
public record CrearSnapshotRequest(
    LocalDate fecha
) {
}
