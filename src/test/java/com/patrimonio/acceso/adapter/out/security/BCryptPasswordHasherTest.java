package com.patrimonio.acceso.adapter.out.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Verifica que las contraseñas se comparen con un hash BCrypt y que nunca se
 * traten como texto plano.
 */
class BCryptPasswordHasherTest {

    private final BCryptPasswordHasher hasher = new BCryptPasswordHasher();

    @Test
    void coincide_conLaContrasenaCorrecta() {
        String hash = new BCryptPasswordEncoder().encode("Demo1234!");

        assertThat(hash).startsWith("$2");
        assertThat(hash).isNotEqualTo("Demo1234!");
        assertThat(hasher.coincide("Demo1234!", hash)).isTrue();
    }

    @Test
    void coincide_conLaContrasenaIncorrecta_devuelveFalse() {
        String hash = new BCryptPasswordEncoder().encode("Demo1234!");

        assertThat(hasher.coincide("otra", hash)).isFalse();
    }

    @Test
    void coincide_conValoresNulosOVacios_devuelveFalse() {
        String hash = new BCryptPasswordEncoder().encode("Demo1234!");

        assertThat(hasher.coincide(null, hash)).isFalse();
        assertThat(hasher.coincide("Demo1234!", null)).isFalse();
        assertThat(hasher.coincide("Demo1234!", "   ")).isFalse();
    }

    @Test
    void coincide_conUnHashInvalido_devuelveFalse() {
        assertThat(hasher.coincide("Demo1234!", "no-es-un-hash")).isFalse();
    }
}
