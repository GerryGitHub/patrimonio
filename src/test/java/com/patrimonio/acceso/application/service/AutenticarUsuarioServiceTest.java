package com.patrimonio.acceso.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.AutenticarUsuarioCommand;
import com.patrimonio.acceso.application.error.CredencialesInvalidasException;
import com.patrimonio.acceso.application.model.CredencialUsuario;
import com.patrimonio.acceso.application.port.out.PasswordHasher;
import com.patrimonio.acceso.application.port.out.UsuarioCredencialesPort;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas del caso de uso de autenticación sin Spring ni base de datos.
 */
@ExtendWith(MockitoExtension.class)
class AutenticarUsuarioServiceTest {

    private static final UUID USUARIO_ID =
        UUID.fromString("99999999-9999-9999-9999-999999999999");
    private static final String EMAIL = "titular@example.com";
    private static final String PASSWORD = "Demo1234!";
    private static final String HASH = "$2a$10$hash-de-prueba";

    @Mock
    private UsuarioCredencialesPort usuarios;

    @Mock
    private PasswordHasher passwordHasher;

    private AutenticarUsuarioService service;

    @BeforeEach
    void setUp() {
        service = new AutenticarUsuarioService(usuarios, passwordHasher);
    }

    @Test
    void execute_conCredencialesValidas_devuelveLaIdentidadMinima() {
        when(usuarios.buscarPorEmail(EMAIL))
            .thenReturn(Optional.of(credencial(true)));
        when(passwordHasher.coincide(PASSWORD, HASH)).thenReturn(true);

        UsuarioAutenticado resultado = service.execute(
            new AutenticarUsuarioCommand(EMAIL, PASSWORD));

        assertThat(resultado.id()).isEqualTo(USUARIO_ID);
        assertThat(resultado.email()).isEqualTo(EMAIL);
        assertThat(resultado.nombre()).isEqualTo("Usuario de prueba");
    }

    @Test
    void execute_normalizaElCorreoAntesDeBuscar() {
        when(usuarios.buscarPorEmail(EMAIL))
            .thenReturn(Optional.of(credencial(true)));
        when(passwordHasher.coincide(PASSWORD, HASH)).thenReturn(true);

        service.execute(new AutenticarUsuarioCommand("  TITULAR@EXAMPLE.COM  ", PASSWORD));

        verify(usuarios).buscarPorEmail(EMAIL);
    }

    @Test
    void execute_cuandoLaContrasenaNoCoincide_lanzaCredencialesInvalidas() {
        when(usuarios.buscarPorEmail(EMAIL))
            .thenReturn(Optional.of(credencial(true)));
        when(passwordHasher.coincide(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.execute(
            new AutenticarUsuarioCommand(EMAIL, "incorrecta")))
            .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void execute_cuandoElUsuarioNoExiste_lanzaCredencialesInvalidas() {
        when(usuarios.buscarPorEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(
            new AutenticarUsuarioCommand(EMAIL, PASSWORD)))
            .isInstanceOf(CredencialesInvalidasException.class);

        verifyNoInteractions(passwordHasher);
    }

    @Test
    void execute_cuandoElUsuarioEstaInactivo_lanzaCredencialesInvalidas() {
        when(usuarios.buscarPorEmail(EMAIL))
            .thenReturn(Optional.of(credencial(false)));

        assertThatThrownBy(() -> service.execute(
            new AutenticarUsuarioCommand(EMAIL, PASSWORD)))
            .isInstanceOf(CredencialesInvalidasException.class);

        verify(passwordHasher, never()).coincide(anyString(), anyString());
    }

    @Test
    void execute_cuandoElCorreoEstaVacio_lanzaCredencialesInvalidas() {
        assertThatThrownBy(() -> service.execute(
            new AutenticarUsuarioCommand("   ", PASSWORD)))
            .isInstanceOf(CredencialesInvalidasException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoLaContrasenaEsNula_lanzaCredencialesInvalidas() {
        assertThatThrownBy(() -> service.execute(
            new AutenticarUsuarioCommand(EMAIL, null)))
            .isInstanceOf(CredencialesInvalidasException.class);

        verifyNoInteractions(usuarios, passwordHasher);
    }

    @Test
    void execute_cuandoElComandoEsNulo_falla() {
        assertThatThrownBy(() -> service.execute(null))
            .isInstanceOf(NullPointerException.class);
    }

    private static CredencialUsuario credencial(boolean activo) {
        return new CredencialUsuario(
            USUARIO_ID, EMAIL, "Usuario de prueba", HASH, activo);
    }
}
