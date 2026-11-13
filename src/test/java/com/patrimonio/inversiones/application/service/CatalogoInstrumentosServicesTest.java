package com.patrimonio.inversiones.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.patrimonio.inversiones.application.port.out.CatalogoInstrumentoRepository;
import com.patrimonio.inversiones.application.view.ClaseActivoView;
import com.patrimonio.inversiones.application.view.TipoInstrumentoView;
import com.patrimonio.inversiones.domain.model.ClaseActivo;
import com.patrimonio.inversiones.domain.model.TipoInstrumento;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas de las consultas de clases y tipos de instrumento.
 */
@ExtendWith(MockitoExtension.class)
class CatalogoInstrumentosServicesTest {

    @Mock
    private CatalogoInstrumentoRepository catalogo;

    private ListarClasesActivoService listarClases;
    private ListarTiposInstrumentoService listarTipos;

    private final UUID claseId = UUID.randomUUID();
    private final UUID otraClaseId = UUID.randomUUID();

    @BeforeEach
    void preparar() {
        listarClases = new ListarClasesActivoService(catalogo);
        listarTipos = new ListarTiposInstrumentoService(catalogo);
    }

    @Test
    void listarClasesMapeaElCatalogoActivo() {
        when(catalogo.listarClasesActivas()).thenReturn(List.of(
            new ClaseActivo(claseId, "Renta variable", "Acciones", true)));

        List<ClaseActivoView> clases = listarClases.execute();

        assertThat(clases).singleElement().satisfies(clase -> {
            assertThat(clase.id()).isEqualTo(claseId);
            assertThat(clase.nombre()).isEqualTo("Renta variable");
            assertThat(clase.activo()).isTrue();
        });
    }

    @Test
    void listarClasesVaciasDevuelveListaVacia() {
        when(catalogo.listarClasesActivas()).thenReturn(List.of());

        assertThat(listarClases.execute()).isEmpty();
    }

    @Test
    void listarTiposSinClaseDevuelveTodosConElNombreDeSuClase() {
        TipoInstrumento accion = tipo(
            claseId, "Acción", "Acciones cotizadas");
        TipoInstrumento bono = tipo(
            otraClaseId, "Bono", "Renta fija");
        when(catalogo.listarTiposActivos()).thenReturn(List.of(accion, bono));
        when(catalogo.buscarClasesPorIds(anyCollection())).thenReturn(List.of(
            new ClaseActivo(claseId, "Renta variable", null, true),
            new ClaseActivo(otraClaseId, "Renta fija", null, true)));

        List<TipoInstrumentoView> tipos = listarTipos.execute(null);

        assertThat(tipos).hasSize(2);
        assertThat(tipos)
            .filteredOn(tipo -> tipo.nombre().equals("Acción"))
            .singleElement()
            .satisfies(tipo -> assertThat(tipo.claseActivoNombre())
                .isEqualTo("Renta variable"));
        assertThat(tipos)
            .filteredOn(tipo -> tipo.nombre().equals("Bono"))
            .singleElement()
            .satisfies(tipo -> assertThat(tipo.claseActivoNombre())
                .isEqualTo("Renta fija"));
        verify(catalogo).listarTiposActivos();
    }

    @Test
    void listarTiposPorClaseFiltraYResuelveElNombre() {
        TipoInstrumento accion = tipo(
            claseId, "Acción", "Acciones cotizadas");
        when(catalogo.listarTiposActivosPorClase(claseId))
            .thenReturn(List.of(accion));
        when(catalogo.buscarClasesPorIds(anyCollection())).thenReturn(List.of(
            new ClaseActivo(claseId, "Renta variable", null, true)));

        List<TipoInstrumentoView> tipos = listarTipos.execute(claseId);

        assertThat(tipos).singleElement().satisfies(tipo -> {
            assertThat(tipo.id()).isEqualTo(accion.id());
            assertThat(tipo.claseActivoId()).isEqualTo(claseId);
            assertThat(tipo.claseActivoNombre())
                .isEqualTo("Renta variable");
        });
        verify(catalogo).listarTiposActivosPorClase(claseId);
    }

    @Test
    void listarTiposSinClaseResueltaDevuelveNombreNulo() {
        TipoInstrumento accion = tipo(
            claseId, "Acción", "Acciones cotizadas");
        when(catalogo.listarTiposActivosPorClase(claseId))
            .thenReturn(List.of(accion));
        when(catalogo.buscarClasesPorIds(anyCollection()))
            .thenReturn(List.of());

        List<TipoInstrumentoView> tipos = listarTipos.execute(claseId);

        assertThat(tipos).singleElement().satisfies(tipo ->
            assertThat(tipo.claseActivoNombre()).isNull());
    }

    @Test
    void listarTiposVaciosDevuelveListaVacia() {
        when(catalogo.listarTiposActivos()).thenReturn(List.of());
        when(catalogo.buscarClasesPorIds(anyCollection()))
            .thenReturn(List.of());

        assertThat(listarTipos.execute(null)).isEmpty();
    }

    private static TipoInstrumento tipo(
        UUID claseActivoId,
        String nombre,
        String descripcion
    ) {
        return new TipoInstrumento(
            UUID.randomUUID(),
            claseActivoId,
            nombre,
            descripcion,
            true);
    }
}
