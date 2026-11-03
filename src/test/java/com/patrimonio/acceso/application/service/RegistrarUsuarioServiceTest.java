package com.patrimonio.acceso.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.RegistrarUsuarioCommand;
import com.patrimonio.acceso.application.error.EmailDuplicadoException;
import com.patrimonio.acceso.application.error.RegistroInvalidoException;
import com.patrimonio.acceso.application.port.out.PasswordHasher;
import com.patrimonio.acceso.application.port.out.UsuarioRegistroPort;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas del registro sin Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class RegistrarUsuarioServiceTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final String NOMBRE = "María Pérez";
    private static final String EMAIL = "maria@example.com";
    private static final String PASSWORD = "Secreta123!";
    private static final String HASH = "$2a$10$hash-de-prueba";

    @Mock
    private UsuarioRegistroPort usuarios;

    @Mock
    private PasswordHasher passwordHasher;

    private RegistrarUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new RegistrarUsuarioService(usuarios, passwordHasher);
    }

    @Test
    void execute_conDatosValidos_creaLaCuentaConHash() {
        when(usuarios.existePorEmail(EMAIL)).thenReturn(false);
        when(passwordHasher.hashear(PASSWORD)).thenReturn(HASH);
        when(usuarios.crear(NOMBRE, EMAIL, HASH))
            .thenReturn(new UsuarioAutenticado(USUARIO_ID, EMAIL, NOMBRE));

        UsuarioAutenticado resultado = service.execute(
            new RegistrarUsuarioCommand(NOMBRE, EMAIL, PASSWORD));

        assertThat(resultado.id()).isEqualTo(USUARIO_ID);
        assertThat(resultado.email()).isEqualTo(EMAIL);
        assertThat(resultado.nombre()).isEqualTo(NOMBRE);
        verify(usuarios).crear(NOMBRE, EMAIL, HASH);
    }

    @Test
    void execute_normalizaElNombreYElCorreoAntesDePersistir() {
        when(usuarios.existePorEmail(EMAIL)).thenReturn(false);
        when(passwordHasher.hashear(PASSWORD)).thenReturn(HASH);
        when(usuarios.crear(NOMBRE, EMAIL, HASH))
            .thenReturn(new UsuarioAutenticado(USUARIO_ID, EMAIL, NOMBRE));

        service.execute(new RegistrarUsuarioCommand(
            "  María Pérez  ",
            "  MARIA@EXAMPLE.COM  ",
            PASSWORD));

        verify(usuarios).existePorEmail(EMAIL);
        verify(usuarios).crear(NOMBRE, EMAIL, HASH);
    }

    @Test
    void execute_cuandoElCorreoYaExiste_lanzaEmailDuplicadoSinHashear() {
        when(usuarios.existePorEmail(EMAIL)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
            new RegistrarUsuarioCommand(NOMBRE, EMAIL, PASSWORD)))
            .isInstanceOf(EmailDuplicadoException.class);

        verify(passwordHasher, never()).hashear(anyString());
        verify(usuarios, never()).crear(anyString(), anyString(), anyString());
    }

    @Test
    void execute_cuandoElNombreEstaVacio_esInvalido() {
        assertThatThrownBy(() -> service.execute(
            new RegistrarUsuarioCommand("   ", EMAIL, PASSWORD)))
            .isInstanceOf(RegistroInvalidoException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoElCorreoNoTieneFormatoValido_esInvalido() {
        assertThatThrownBy(() -> service.execute(
            new RegistrarUsuarioCommand(NOMBRE, "no-es-un-correo", PASSWORD)))
            .isInstanceOf(RegistroInvalidoException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoLaContrasenaEsCorta_esInvalida() {
        assertThatThrownBy(() -> service.execute(
            new RegistrarUsuarioCommand(NOMBRE, EMAIL, "corta")))
            .isInstanceOf(RegistroInvalidoException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoLaContrasenaSuperaLos72Bytes_esInvalida() {
        String passwordLarga = "á".repeat(37);

        assertThatThrownBy(() -> service.execute(
            new RegistrarUsuarioCommand(NOMBRE, EMAIL, passwordLarga)))
            .isInstanceOf(RegistroInvalidoException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoLaContrasenaEsSoloEspacios_esInvalida() {
        assertThatThrownBy(() -> service.execute(
            new RegistrarUsuarioCommand(NOMBRE, EMAIL, "        ")))
            .isInstanceOf(RegistroInvalidoException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoElComandoEsNulo_falla() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(NullPointerException.class);
    }
}
