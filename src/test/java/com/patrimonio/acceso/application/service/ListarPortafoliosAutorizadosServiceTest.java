package com.patrimonio.acceso.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.api.dto.PortafolioResumen;
import com.patrimonio.acceso.api.port.AccesoQueryPort;
import com.patrimonio.acceso.application.query.ListarPortafoliosAutorizadosQuery;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas del caso de uso que lista portafolios autorizados.
 */
@ExtendWith(MockitoExtension.class)
class ListarPortafoliosAutorizadosServiceTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final UUID PORTAFOLIO_ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private AccesoQueryPort accesoQueryPort;

    private ListarPortafoliosAutorizadosService service;

    @BeforeEach
    void setUp() {
        service = new ListarPortafoliosAutorizadosService(accesoQueryPort);
    }

    @Test
    void execute_devuelveSoloLoQueAutorizaElPuerto() {
        PortafolioResumen portafolio = new PortafolioResumen(
            PORTAFOLIO_ID,
            "Portafolio principal",
            "Descripción principal",
            new MonedaResumen(
                UUID.randomUUID(),
                "USD",
                "Dólar estadounidense",
                "$",
                2),
            true);
        when(accesoQueryPort.listarAutorizados(USUARIO_ID))
            .thenReturn(List.of(portafolio));

        List<PortafolioResumen> resultado = service.execute(
            new ListarPortafoliosAutorizadosQuery(USUARIO_ID));

        assertThat(resultado).containsExactly(portafolio);
        verify(accesoQueryPort).listarAutorizados(USUARIO_ID);
    }

    @Test
    void execute_cuandoElUsuarioNoTienePortafolios_devuelveListaVacia() {
        when(accesoQueryPort.listarAutorizados(USUARIO_ID))
            .thenReturn(List.of());

        assertThat(service.execute(
            new ListarPortafoliosAutorizadosQuery(USUARIO_ID))).isEmpty();
    }

    @Test
    void execute_cuandoLaConsultaEsNula_falla() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(NullPointerException.class);
    }
}
