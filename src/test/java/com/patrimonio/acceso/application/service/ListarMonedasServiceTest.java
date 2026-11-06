package com.patrimonio.acceso.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.MonedaResumen;
import com.patrimonio.acceso.application.port.out.MonedaQueryPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas del caso de uso que lista el catálogo de monedas.
 */
@ExtendWith(MockitoExtension.class)
class ListarMonedasServiceTest {

    @Mock
    private MonedaQueryPort monedas;

    private ListarMonedasService service;

    @BeforeEach
    void setUp() {
        service = new ListarMonedasService(monedas);
    }

    @Test
    void execute_devuelveElCatalogoDelPuerto() {
        List<MonedaResumen> catalogo = List.of(
            new MonedaResumen(
                UUID.randomUUID(), "EUR", "Euro", "€", 2),
            new MonedaResumen(
                UUID.randomUUID(), "USD", "Dólar estadounidense", "$", 2));
        when(monedas.listarActivas()).thenReturn(catalogo);

        assertThat(service.execute()).isEqualTo(catalogo);
        verify(monedas).listarActivas();
    }
}
