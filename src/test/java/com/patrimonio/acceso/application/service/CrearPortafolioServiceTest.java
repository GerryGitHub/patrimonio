package com.patrimonio.acceso.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.application.command.CrearPortafolioCommand;
import com.patrimonio.acceso.application.error.MonedaInvalidaException;
import com.patrimonio.acceso.application.error.PortafolioInvalidoException;
import com.patrimonio.acceso.application.port.out.MonedaQueryPort;
import com.patrimonio.acceso.application.port.out.PortafolioRegistroPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas del caso de uso de creación de portafolios.
 */
@ExtendWith(MockitoExtension.class)
class CrearPortafolioServiceTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID MONEDA_ID =
        UUID.fromString("77777777-7777-7777-7777-777777777777");

    private static final MonedaResumen USD = new MonedaResumen(
        MONEDA_ID,
        "USD",
        "Dólar estadounidense",
        "$",
        2);

    @Mock
    private MonedaQueryPort monedas;

    @Mock
    private PortafolioRegistroPort portafolios;

    private CrearPortafolioService service;

    @BeforeEach
    void setUp() {
        service = new CrearPortafolioService(monedas, portafolios);
    }

    @Test
    void execute_normalizaYDelegaEnElPuertoDePersistencia() {
        PortafolioResumen creado = new PortafolioResumen(
            UUID.randomUUID(),
            "Largo plazo",
            "Ahorro para la jubilación",
            USD,
            true);
        when(monedas.buscarActivaPorId(MONEDA_ID)).thenReturn(Optional.of(USD));
        when(portafolios.crear(
            USUARIO_ID,
            "Largo plazo",
            "Ahorro para la jubilación",
            USD)).thenReturn(creado);

        PortafolioResumen resultado = service.execute(new CrearPortafolioCommand(
            USUARIO_ID,
            "  Largo plazo  ",
            "  Ahorro para la jubilación  ",
            MONEDA_ID));

        assertThat(resultado).isEqualTo(creado);
        verify(portafolios).crear(
            USUARIO_ID,
            "Largo plazo",
            "Ahorro para la jubilación",
            USD);
    }

    @Test
    void execute_conDescripcionVaciaLaConvierteEnNula() {
        when(monedas.buscarActivaPorId(MONEDA_ID)).thenReturn(Optional.of(USD));
        when(portafolios.crear(any(), any(), any(), any()))
            .thenReturn(new PortafolioResumen(
                UUID.randomUUID(), "Corto plazo", null, USD, true));

        service.execute(new CrearPortafolioCommand(
            USUARIO_ID, "Corto plazo", "   ", MONEDA_ID));

        ArgumentCaptor<String> descripcion =
            ArgumentCaptor.forClass(String.class);
        verify(portafolios).crear(
            org.mockito.ArgumentMatchers.eq(USUARIO_ID),
            org.mockito.ArgumentMatchers.eq("Corto plazo"),
            descripcion.capture(),
            org.mockito.ArgumentMatchers.eq(USD));
        assertThat(descripcion.getValue()).isNull();
    }

    @Test
    void execute_cuandoElNombreEstaVacio_fallaSinConsultarLaMoneda() {
        assertThatThrownBy(() -> service.execute(new CrearPortafolioCommand(
            USUARIO_ID, "   ", null, MONEDA_ID)))
            .isInstanceOf(PortafolioInvalidoException.class);

        verify(monedas, never()).buscarActivaPorId(any());
        verify(portafolios, never()).crear(any(), any(), any(), any());
    }

    @Test
    void execute_cuandoLaDescripcionSuperaLos500Caracteres_falla() {
        assertThatThrownBy(() -> service.execute(new CrearPortafolioCommand(
            USUARIO_ID, "Portafolio", "a".repeat(501), MONEDA_ID)))
            .isInstanceOf(PortafolioInvalidoException.class);
    }

    @Test
    void execute_cuandoLaMonedaNoExisteOEstaInactiva_falla() {
        when(monedas.buscarActivaPorId(MONEDA_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(new CrearPortafolioCommand(
            USUARIO_ID, "Portafolio", null, MONEDA_ID)))
            .isInstanceOf(MonedaInvalidaException.class);

        verify(portafolios, never()).crear(any(), any(), any(), any());
    }

    @Test
    void execute_cuandoLaMonedaEsNula_fallaConValidacion() {
        assertThatThrownBy(() -> service.execute(new CrearPortafolioCommand(
            USUARIO_ID, "Portafolio", null, null)))
            .isInstanceOf(PortafolioInvalidoException.class);
    }

    @Test
    void execute_cuandoElComandoEsNulo_falla() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(NullPointerException.class);
    }
}
