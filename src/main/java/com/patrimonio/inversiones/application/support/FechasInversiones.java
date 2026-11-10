package com.patrimonio.inversiones.application.support;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/**
 * Conversión de fechas entre el borde HTTP y el dominio.
 *
 * <p>La API recibe y devuelve {@link LocalDate}; el dominio y la base usan
 * {@link Instant} en UTC. La fecha de apertura se interpreta al inicio del día
 * UTC para que la comparación con la fecha actual sea determinista.</p>
 */
public final class FechasInversiones {

    private FechasInversiones() {
    }

    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    public static LocalDate hoy() {
        return LocalDate.now(ZoneOffset.UTC);
    }

    public static LocalDate fecha(Instant instante) {
        return instante.atZone(ZoneOffset.UTC).toLocalDate();
    }
}
