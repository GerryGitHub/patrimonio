package com.patrimonio;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada del backend de Patrimonio.
 *
 * <p>La aplicación es un monolito modular. Los módulos de negocio se añadirán
 * con su propio dominio, aplicación y adaptadores; esta clase solo arranca el
 * contexto de Spring Boot.</p>
 */
@SpringBootApplication
public class PatrimonioApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatrimonioApplication.class, args);
    }
}
