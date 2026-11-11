package com.patrimonio.consolidacion.application.support;

import com.patrimonio.consolidacion.domain.model.GranularidadEvolucion;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Genera las fechas de corte de una serie histórica.
 *
 * <p>Las fechas nunca son anteriores al inicio solicitado ni posteriores a la
 * fecha final. El último punto siempre coincide con la fecha final para que la
 * gráfica termine en el dato actual.</p>
 */
public final class CalculadoraEvolucion {

    private CalculadoraEvolucion() {
    }

    public static List<LocalDate> fechas(
        GranularidadEvolucion granularidad,
        LocalDate desde,
        LocalDate hasta
    ) {
        if (desde == null || hasta == null || desde.isAfter(hasta)) {
            return List.of();
        }
        List<LocalDate> fechas = new ArrayList<>();
        switch (granularidad) {
            case DIARIA -> {
                LocalDate cursor = desde;
                while (!cursor.isAfter(hasta)) {
                    fechas.add(cursor);
                    cursor = cursor.plusDays(1);
                }
            }
            case SEMANAL -> {
                LocalDate cursor = desde;
                while (!cursor.isAfter(hasta)) {
                    fechas.add(cursor);
                    cursor = cursor.plusWeeks(1);
                }
                agregarCierre(fechas, hasta);
            }
            case MENSUAL -> {
                LocalDate cursor = desde.withDayOfMonth(1)
                    .plusMonths(1)
                    .minusDays(1);
                while (cursor.isBefore(hasta)) {
                    fechas.add(cursor);
                    cursor = cursor.withDayOfMonth(1)
                        .plusMonths(2)
                        .minusDays(1);
                }
                agregarCierre(fechas, hasta);
            }
        }
        return fechas;
    }

    private static void agregarCierre(List<LocalDate> fechas, LocalDate hasta) {
        if (fechas.isEmpty() || !fechas.get(fechas.size() - 1).equals(hasta)) {
            fechas.add(hasta);
        }
    }
}
