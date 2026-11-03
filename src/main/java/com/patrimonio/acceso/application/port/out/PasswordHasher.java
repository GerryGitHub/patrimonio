package com.patrimonio.acceso.application.port.out;

/**
 * Verificación de contraseñas contra un hash adaptativo.
 *
 * <p>La implementación concreta vive en infraestructura. El caso de uso nunca
 * ve el algoritmo ni recibe la contraseña en claro más allá de esta llamada.</p>
 */
public interface PasswordHasher {

    /**
     * Indica si la contraseña en claro corresponde al hash almacenado.
     *
     * @return {@code false} si algún valor es nulo o el hash no es válido; no
     *     lanza excepciones por credenciales incorrectas
     */
    boolean coincide(String passwordEnClaro, String hash);
}
