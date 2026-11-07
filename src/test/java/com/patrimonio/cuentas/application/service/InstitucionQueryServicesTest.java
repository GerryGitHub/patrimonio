package com.patrimonio.cuentas.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.application.query.ListarInstitucionesQuery;
import com.patrimonio.cuentas.application.view.InstitucionView;
import com.patrimonio.cuentas.domain.model.Institucion;
import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import com.patrimonio.shared.domain.error.NotFoundException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InstitucionQueryServicesTest {

    private static final UUID ID =
        UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private InstitucionRepository instituciones;

    private ListarInstitucionesService listar;
    private ConsultarInstitucionService consultar;

    @BeforeEach
    void setUp() {
        listar = new ListarInstitucionesService(instituciones);
        consultar = new ConsultarInstitucionService(instituciones);
    }

    @Test
    void listar_aplicaLosFiltrosYDevuelveLasVistas() {
        when(instituciones.listar(TipoInstitucion.SOFIPO, true))
            .thenReturn(List.of(institucion()));

        List<InstitucionView> resultado = listar.execute(
            new ListarInstitucionesQuery(TipoInstitucion.SOFIPO, true));

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).nombre()).isEqualTo("SOFIPO de prueba");
        verify(instituciones).listar(TipoInstitucion.SOFIPO, true);
    }

    @Test
    void listar_sinFiltros_devuelveListaVacia() {
        when(instituciones.listar(null, null)).thenReturn(List.of());

        assertThat(listar.execute(new ListarInstitucionesQuery(null, null))).isEmpty();
    }

    @Test
    void consultar_cuandoExiste_devuelveLaVista() {
        when(instituciones.buscarPorId(ID)).thenReturn(Optional.of(institucion()));

        InstitucionView vista = consultar.execute(ID);

        assertThat(vista.id()).isEqualTo(ID);
        assertThat(vista.tipo()).isEqualTo(TipoInstitucion.SOFIPO);
    }

    @Test
    void consultar_cuandoNoExiste_lanzaNotFound() {
        when(instituciones.buscarPorId(ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> consultar.execute(ID))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void consultar_conIdNulo_lanzaNotFound() {
        assertThatThrownBy(() -> consultar.execute(null))
            .isInstanceOf(NotFoundException.class);
    }

    private static Institucion institucion() {
        return Institucion.reconstruir(
            ID,
            "SOFIPO de prueba",
            TipoInstitucion.SOFIPO,
            "México",
            null,
            true);
    }
}
