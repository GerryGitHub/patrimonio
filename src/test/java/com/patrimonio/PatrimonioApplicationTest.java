package com.patrimonio;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;

class PatrimonioApplicationTest {

    @Test
    void applicationClassIsAnnotatedWithSpringBootApplication() {
        assertThat(PatrimonioApplication.class)
            .hasAnnotation(SpringBootApplication.class);
    }
}
