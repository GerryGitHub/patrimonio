package com.patrimonio.acceso.application.service;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.AutenticarUsuarioCommand;
import com.patrimonio.acceso.application.error.CredencialesInvalidasException;
import com.patrimonio.acceso.application.model.CredencialUsuario;
import com.patrimonio.acceso.application.port.in.AutenticarUsuarioUseCase;
import com.patrimonio.acceso.application.port.out.PasswordHasher;
import com.patrimonio.acceso.application.port.out.UsuarioCredencialesPort;
import java.util.Locale;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del caso de uso de autenticación.
 *
 * <p>No conoce Spring Security, HTTP ni sesiones. Normaliza el correo, verifica
 * el hash con el puerto de contraseñas y devuelve la identidad mínima. Los
 * fallos se traducen a una única excepción para no distinguir entre usuario
 * inexistente, contraseña incorrecta y cuenta inactiva.</p>
 */
@Service
public class AutenticarUsuarioService implements AutenticarUsuarioUseCase {

    private final UsuarioCredencialesPort usuarios;
    private final PasswordHasher passwordHasher;

    public AutenticarUsuarioService(
        UsuarioCredencialesPort usuarios,
        PasswordHasher passwordHasher
    ) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioAutenticado execute(AutenticarUsuarioCommand command) {
        Objects.requireNonNull(command, "command");

        String email = normalizar(command.usuario());
        String password = command.password();
        if (email.isEmpty() || password == null || password.isEmpty()) {
            throw new CredencialesInvalidasException();
        }

        CredencialUsuario credencial = usuarios.buscarPorEmail(email)
            .orElseThrow(CredencialesInvalidasException::new);

        if (!credencial.activo()
            || !passwordHasher.coincide(password, credencial.passwordHash())) {
            throw new CredencialesInvalidasException();
        }

        return new UsuarioAutenticado(
            credencial.id(),
            credencial.email(),
            credencial.nombre());
    }

    private static String normalizar(String usuario) {
        return usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT);
    }
}
