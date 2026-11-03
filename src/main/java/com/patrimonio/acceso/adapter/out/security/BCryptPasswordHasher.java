package com.patrimonio.acceso.adapter.out.security;

import com.patrimonio.acceso.application.port.out.PasswordHasher;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Verificación de contraseñas con BCrypt.
 *
 * <p>BCrypt es un hash adaptativo con sal incorporada; el coste por defecto de
 * Spring Security se mantiene para este MVP. Nunca se cifra ni se guarda la
 * contraseña en claro.</p>
 */
@Component
public class BCryptPasswordHasher implements PasswordHasher {

    private final PasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    public boolean coincide(String passwordEnClaro, String hash) {
        if (passwordEnClaro == null || hash == null || hash.isBlank()) {
            return false;
        }
        return encoder.matches(passwordEnClaro, hash);
    }

    @Override
    public String hashear(String passwordEnClaro) {
        if (passwordEnClaro == null || passwordEnClaro.isBlank()) {
            throw new IllegalArgumentException(
                "La contraseña no puede estar vacía.");
        }
        return encoder.encode(passwordEnClaro);
    }
}
