package com.patrimonio.acceso.application.service;

import com.patrimonio.acceso.api.dto.UsuarioAutenticado;
import com.patrimonio.acceso.application.command.RegistrarUsuarioCommand;
import com.patrimonio.acceso.application.error.EmailDuplicadoException;
import com.patrimonio.acceso.application.error.RegistroInvalidoException;
import com.patrimonio.acceso.application.port.in.RegistrarUsuarioUseCase;
import com.patrimonio.acceso.application.port.out.PasswordHasher;
import com.patrimonio.acceso.application.port.out.UsuarioRegistroPort;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementación del registro de usuarios.
 *
 * <p>Normaliza el correo y el nombre, comprueba duplicados, hashea la
 * contraseña con el puerto de infraestructura y persiste la cuenta. No conoce
 * HTTP ni crea sesiones. El límite de 72 bytes de BCrypt se valida aquí para
 * que el adaptador no reciba contraseñas que el algoritmo truncaría.</p>
 */
@Service
public class RegistrarUsuarioService implements RegistrarUsuarioUseCase {

    private static final int NOMBRE_MAXIMO = 150;
    private static final int EMAIL_MAXIMO = 254;
    private static final int PASSWORD_MINIMO = 8;
    private static final int PASSWORD_MAXIMO_BYTES = 72;
    private static final Pattern EMAIL =
        Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UsuarioRegistroPort usuarios;
    private final PasswordHasher passwordHasher;

    public RegistrarUsuarioService(
        UsuarioRegistroPort usuarios,
        PasswordHasher passwordHasher
    ) {
        this.usuarios = usuarios;
        this.passwordHasher = passwordHasher;
    }

    @Override
    @Transactional
    public UsuarioAutenticado execute(RegistrarUsuarioCommand command) {
        Objects.requireNonNull(command, "command");

        String nombre = normalizarNombre(command.nombre());
        String email = normalizarEmail(command.email());
        String password = command.password();

        validar(nombre, email, password);

        if (usuarios.existePorEmail(email)) {
            throw new EmailDuplicadoException();
        }

        String passwordHash = passwordHasher.hashear(password);
        return usuarios.crear(nombre, email, passwordHash);
    }

    private static void validar(String nombre, String email, String password) {
        if (nombre.isEmpty() || nombre.length() > NOMBRE_MAXIMO) {
            throw new RegistroInvalidoException();
        }
        if (email.isEmpty()
            || email.length() > EMAIL_MAXIMO
            || !EMAIL.matcher(email).matches()) {
            throw new RegistroInvalidoException();
        }
        if (password == null
            || password.isBlank()
            || password.length() < PASSWORD_MINIMO
            || password.getBytes(StandardCharsets.UTF_8).length
                > PASSWORD_MAXIMO_BYTES) {
            throw new RegistroInvalidoException();
        }
    }

    private static String normalizarNombre(String nombre) {
        return nombre == null ? "" : nombre.strip();
    }

    private static String normalizarEmail(String email) {
        return email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    }
}
