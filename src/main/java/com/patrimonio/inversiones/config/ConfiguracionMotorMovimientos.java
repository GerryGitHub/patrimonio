package com.patrimonio.inversiones.config;

import com.patrimonio.inversiones.domain.model.MotorMovimientos;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del motor de movimientos.
 *
 * <p>El motor es dominio puro. Se publica como bean para que los casos de uso
 * compartan la misma instancia y para poder sustituir sus estrategias en una
 * prueba de integración.</p>
 */
@Configuration
public class ConfiguracionMotorMovimientos {

    @Bean
    public MotorMovimientos motorMovimientos() {
        return MotorMovimientos.estandar();
    }
}
