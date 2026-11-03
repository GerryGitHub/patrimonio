package com.patrimonio.cuentas.adapter.in.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

/**
 * Comprueba el bloqueo de arranque que impide exponer el endpoint de cuentas
 * con el perfil {@code prod}.
 */
class CuentasApiExposicionGuardTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
        .withUserConfiguration(CuentasApiExposicionGuard.class)
        .withInitializer(context ->
            context.getEnvironment().setActiveProfiles("prod"));

    @Test
    void fallaElArranqueSiSeIntentaExponerElEndpointEnProduccion() {
        runner.withPropertyValues("patrimonio.api.cuentas.expuesta=true")
            .run(context -> {
                assertThat(context).hasFailed();
                assertThat(context.getStartupFailure())
                    .hasStackTraceContaining(
                        "no puede exponerse con el perfil 'prod'");
            });
    }

    @Test
    void arrancaEnProduccionSiElEndpointPermaneceDeshabilitado() {
        runner.withPropertyValues("patrimonio.api.cuentas.expuesta=false")
            .run(context -> assertThat(context).hasNotFailed());
    }

    @Test
    void arrancaEnProduccionSiLaPropiedadNoEstaDefinida() {
        runner.run(context -> assertThat(context).hasNotFailed());
    }
}
