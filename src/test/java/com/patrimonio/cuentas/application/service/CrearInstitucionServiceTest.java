package com.patrimonio.cuentas.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.cuentas.application.command.CrearInstitucionCommand;
import com.patrimonio.cuentas.application.port.out.InstitucionRepository;
import com.patrimonio.cuentas.application.view.InstitucionView;
import com.patrimonio.cuentas.domain.error.InstitucionDuplicadaException;
import com.patrimonio.cuentas.domain.error.InstitucionInvalidaException;
import com.patrimonio.cuentas.domain.model.Institucion;
import com.patrimonio.cuentas.domain.model.TipoInstitucion;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CrearInstitucionServiceTest {

    @Mock
    private InstitucionRepository instituciones;

    private CrearInstitucionService service;

    @BeforeEach
    void setUp() {
        service = new CrearInstitucionService(instituciones);
    }

    @Test
    void execute_normalizaYGuardaLaInstitucion() {
        when(instituciones.existePorNombre("Banco del Norte")).thenReturn(false);
        when(instituciones.guardar(any(Institucion.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        InstitucionView vista = service.execute(new CrearInstitucionCommand(
            "  Banco   del Norte ", TipoInstitucion.BANCO, " México ", " https://banco.mx "));

        ArgumentCaptor<Institucion> captor = ArgumentCaptor.forClass(Institucion.class);
        verify(instituciones).guardar(captor.capture());
        Institucion guardada = captor.getValue();
        assertThat(guardada.nombre()).isEqualTo("Banco del Norte");
        assertThat(guardada.tipo()).isEqualTo(TipoInstitucion.BANCO);
        assertThat(guardada.pais()).contains("México");
        assertThat(guardada.sitioWeb()).contains("https://banco.mx");
        assertThat(guardada.activo()).isTrue();
        assertThat(vista.nombre()).isEqualTo("Banco del Norte");
    }

    @Test
    void execute_conNombreDuplicado_falla() {
        when(instituciones.existePorNombre("Banco del Norte")).thenReturn(true);

        assertThatThrownBy(() -> service.execute(new CrearInstitucionCommand(
            "Banco del Norte", TipoInstitucion.BANCO, null, null)))
            .isInstanceOf(InstitucionDuplicadaException.class);
    }

    @Test
    void execute_sinCategoria_falla() {
        assertThatThrownBy(() -> service.execute(new CrearInstitucionCommand(
            "Banco del Norte", null, null, null)))
            .isInstanceOf(InstitucionInvalidaException.class);
        verifyNoInteractions(instituciones);
    }

    @Test
    void execute_conNombreEnBlanco_falla() {
        assertThatThrownBy(() -> service.execute(new CrearInstitucionCommand(
            "   ", TipoInstitucion.BANCO, null, null)))
            .isInstanceOf(InstitucionInvalidaException.class);
        verifyNoInteractions(instituciones);
    }

    @Test
    void execute_conNombreDemasiadoLargo_falla() {
        assertThatThrownBy(() -> service.execute(new CrearInstitucionCommand(
            "x".repeat(151), TipoInstitucion.BANCO, null, null)))
            .isInstanceOf(InstitucionInvalidaException.class);
        verifyNoInteractions(instituciones);
    }

    @Test
    void execute_conComandoNulo_falla() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(NullPointerException.class);
        verifyNoInteractions(instituciones);
    }

    @Test
    void execute_conPaisYSitioVaciosLosConvierteEnNulos() {
        when(instituciones.existePorNombre("Banco")).thenReturn(false);
        when(instituciones.guardar(any(Institucion.class)))
            .thenAnswer(invocacion -> invocacion.getArgument(0));

        InstitucionView vista = service.execute(new CrearInstitucionCommand(
            "Banco", TipoInstitucion.BANCO, "  ", " "));

        assertThat(vista.pais()).isNull();
        assertThat(vista.sitioWeb()).isNull();
    }
}
