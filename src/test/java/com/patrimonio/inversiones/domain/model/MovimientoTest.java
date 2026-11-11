package com.patrimonio.inversiones.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.patrimonio.inversiones.domain.error.MovimientoInvalidoException;
import com.patrimonio.inversiones.domain.error.MovimientoNoReversibleException;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del motor de movimientos y del agregado inmutable de movimiento.
 */
class MovimientoTest {

    private static final UUID PORTAFOLIO =
        UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CUENTA =
        UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID POSICION =
        UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID INSTRUMENTO =
        UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID MONEDA =
        UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID USUARIO =
        UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final Instant AHORA = Instant.parse("2026-02-10T12:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 2, 10);

    private final MotorMovimientos motor = MotorMovimientos.estandar();

    @Test
    void apertura_incrementaCantidadYCosto() {
        Movimiento movimiento = registrar(
            TipoMovimiento.APERTURA,
            "10", "25", "0", "0", null,
            "0", "0");

        assertThat(movimiento.cantidadResultante())
            .isEqualByComparingTo("10");
        assertThat(movimiento.costoResultante())
            .isEqualByComparingTo("250");
        assertThat(movimiento.efectoCantidad()).isEqualByComparingTo("10");
        assertThat(movimiento.efectoCosto()).isEqualByComparingTo("250");
        assertThat(movimiento.resultadoRealizado()).isEmpty();
    }

    @Test
    void compra_incluyeComisionesEImpuestosEnElCosto() {
        Movimiento movimiento = registrar(
            TipoMovimiento.COMPRA,
            "5", "20", "1", "0.5", null,
            "10", "100");

        assertThat(movimiento.importeBruto()).isEqualByComparingTo("100");
        assertThat(movimiento.importeNeto()).isEqualByComparingTo("101.5");
        assertThat(movimiento.cantidadResultante())
            .isEqualByComparingTo("15");
        assertThat(movimiento.costoResultante())
            .isEqualByComparingTo("201.5");
    }

    @Test
    void venta_usaPromedioPonderadoYCalculaResultadoRealizado() {
        Movimiento movimiento = registrar(
            TipoMovimiento.VENTA,
            "4", "30", "2", "1", null,
            "10", "250");

        assertThat(movimiento.importeBruto()).isEqualByComparingTo("120");
        assertThat(movimiento.importeNeto()).isEqualByComparingTo("117");
        assertThat(movimiento.costoAplicado()).isEqualByComparingTo("100");
        assertThat(movimiento.resultadoRealizado().orElseThrow())
            .isEqualByComparingTo("17");
        assertThat(movimiento.cantidadResultante())
            .isEqualByComparingTo("6");
        assertThat(movimiento.costoResultante())
            .isEqualByComparingTo("150");
    }

    @Test
    void venta_queSuperaLaCantidadDisponible_falla() {
        assertThatThrownBy(() -> registrar(
            TipoMovimiento.VENTA,
            "11", "30", "0", "0", null,
            "10", "250"))
            .isInstanceOf(MovimientoInvalidoException.class)
            .hasMessageContaining("supera la cantidad disponible");
    }

    @Test
    void venta_total_retiraTodoElCosto() {
        Movimiento movimiento = registrar(
            TipoMovimiento.VENTA,
            "10", "30", "0", "0", null,
            "10", "250");

        assertThat(movimiento.cantidadResultante())
            .isEqualByComparingTo("0");
        assertThat(movimiento.costoResultante())
            .isEqualByComparingTo("0");
        assertThat(movimiento.resultadoRealizado().orElseThrow())
            .isEqualByComparingTo("50");
    }

    @Test
    void ajustePositivo_requiereCostoExplicito() {
        assertThatThrownBy(() -> registrar(
            TipoMovimiento.AJUSTE_POSITIVO,
            "2", "10", "0", "0", null,
            "10", "100"))
            .isInstanceOf(MovimientoInvalidoException.class)
            .hasMessageContaining("costo total ajustado");
    }

    @Test
    void ajustePositivo_sumaCantidadYCostoIndicados() {
        Movimiento movimiento = registrar(
            TipoMovimiento.AJUSTE_POSITIVO,
            "2", "10", "0", "0", "15",
            "10", "100");

        assertThat(movimiento.cantidadResultante())
            .isEqualByComparingTo("12");
        assertThat(movimiento.costoResultante())
            .isEqualByComparingTo("115");
        assertThat(movimiento.efectoCosto()).isEqualByComparingTo("15");
    }

    @Test
    void ajusteNegativoQueDejaCantidadCero_debeRetirarTodoElCosto() {
        assertThatThrownBy(() -> registrar(
            TipoMovimiento.AJUSTE_NEGATIVO,
            "10", "10", "0", "0", "50",
            "10", "100"))
            .isInstanceOf(MovimientoInvalidoException.class)
            .hasMessageContaining("retirar todo el costo");
    }

    @Test
    void reversionDeCompra_invierteLosEfectos() {
        Movimiento compra = registrar(
            TipoMovimiento.COMPRA,
            "5", "20", "1", "0.5", null,
            "10", "100");

        Movimiento reversion = Movimiento.reversion(
            compra,
            UUID.randomUUID(),
            HOY,
            "Corrección",
            USUARIO,
            null,
            new BigDecimal("15"),
            new BigDecimal("201.5"),
            motor,
            AHORA);

        assertThat(reversion.cantidadResultante())
            .isEqualByComparingTo("10");
        assertThat(reversion.costoResultante())
            .isEqualByComparingTo("100");
        assertThat(reversion.movimientoOrigenId()).contains(compra.id());
    }

    @Test
    void reversionDeVenta_restauraCantidadCostoYResultado() {
        Movimiento venta = registrar(
            TipoMovimiento.VENTA,
            "4", "30", "2", "1", null,
            "10", "250");

        Movimiento reversion = Movimiento.reversion(
            venta,
            UUID.randomUUID(),
            HOY,
            "Corrección",
            USUARIO,
            null,
            new BigDecimal("6"),
            new BigDecimal("150"),
            motor,
            AHORA);

        assertThat(reversion.cantidadResultante())
            .isEqualByComparingTo("10");
        assertThat(reversion.costoResultante())
            .isEqualByComparingTo("250");
        assertThat(reversion.resultadoRealizado().orElseThrow())
            .isEqualByComparingTo("-17");
    }

    @Test
    void unaReversionNoPuedeRevertirse() {
        Movimiento venta = registrar(
            TipoMovimiento.VENTA,
            "4", "30", "0", "0", null,
            "10", "250");
        Movimiento reversion = Movimiento.reversion(
            venta,
            UUID.randomUUID(),
            HOY,
            null,
            USUARIO,
            null,
            new BigDecimal("6"),
            new BigDecimal("150"),
            motor,
            AHORA);

        assertThatThrownBy(() -> Movimiento.reversion(
            reversion,
            UUID.randomUUID(),
            HOY,
            null,
            USUARIO,
            null,
            new BigDecimal("10"),
            new BigDecimal("250"),
            motor,
            AHORA))
            .isInstanceOf(MovimientoNoReversibleException.class);
    }

    @Test
    void fechaDeOperacionFutura_falla() {
        assertThatThrownBy(() -> Movimiento.registrar(
            solicitud(
                TipoMovimiento.COMPRA,
                "1", "10", "0", "0", null,
                HOY.plusDays(1)),
            new BigDecimal("0"),
            new BigDecimal("0"),
            motor,
            AHORA))
            .isInstanceOf(MovimientoInvalidoException.class)
            .hasMessageContaining("posterior a hoy");
    }

    @Test
    void liquidacionAnteriorALaOperacion_falla() {
        SolicitudMovimiento base = solicitud(
            TipoMovimiento.COMPRA,
            "1", "10", "0", "0", null,
            HOY);
        SolicitudMovimiento invalida = new SolicitudMovimiento(
            base.id(),
            base.portafolioId(),
            base.cuentaId(),
            base.posicionId(),
            base.instrumentoId(),
            base.tipo(),
            base.fechaOperacion(),
            HOY.minusDays(1),
            base.cantidad(),
            base.precioUnitario(),
            base.comisiones(),
            base.impuestos(),
            base.monedaId(),
            base.tipoCambio(),
            base.costoTotalAjuste(),
            base.observaciones(),
            base.usuarioId(),
            base.claveIdempotencia());

        assertThatThrownBy(() -> Movimiento.registrar(
            invalida,
            new BigDecimal("0"),
            new BigDecimal("0"),
            motor,
            AHORA))
            .isInstanceOf(MovimientoInvalidoException.class)
            .hasMessageContaining("liquidación");
    }

    @Test
    void unaReversionNoSeRegistraDirectamente() {
        assertThatThrownBy(() -> registrar(
            TipoMovimiento.REVERSION,
            "1", "10", "0", "0", null,
            "10", "100"))
            .isInstanceOf(MovimientoInvalidoException.class)
            .hasMessageContaining("no se registra directamente");
    }

    private Movimiento registrar(
        TipoMovimiento tipo,
        String cantidad,
        String precio,
        String comisiones,
        String impuestos,
        String costoAjuste,
        String cantidadActual,
        String costoActual
    ) {
        return Movimiento.registrar(
            solicitud(
                tipo,
                cantidad,
                precio,
                comisiones,
                impuestos,
                costoAjuste,
                HOY),
            new BigDecimal(cantidadActual),
            new BigDecimal(costoActual),
            motor,
            AHORA);
    }

    private static SolicitudMovimiento solicitud(
        TipoMovimiento tipo,
        String cantidad,
        String precio,
        String comisiones,
        String impuestos,
        String costoAjuste,
        LocalDate fecha
    ) {
        return new SolicitudMovimiento(
            UUID.randomUUID(),
            PORTAFOLIO,
            CUENTA,
            POSICION,
            INSTRUMENTO,
            tipo,
            fecha,
            null,
            new BigDecimal(cantidad),
            new BigDecimal(precio),
            new BigDecimal(comisiones),
            new BigDecimal(impuestos),
            MONEDA,
            BigDecimal.ONE,
            costoAjuste == null ? null : new BigDecimal(costoAjuste),
            null,
            USUARIO,
            null);
    }
}
