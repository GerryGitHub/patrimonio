package com.patrimonio;

import static org.assertj.core.api.Assertions.assertThat;

import com.patrimonio.testsupport.PostgresTestContainer;
import jakarta.persistence.EntityManagerFactory;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Verifica que el contexto completo arranca contra un PostgreSQL 16 efímero y
 * que Hibernate valida los mapeos con {@code ddl-auto=validate}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class PatrimonioApplicationIT {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer POSTGRESQL = PostgresTestContainer.nuevo();

    @Autowired
    private DataSource dataSource;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Environment environment;

    @Autowired
    private EntityManagerFactory entityManagerFactory;

    @Test
    void contextLoadsAndUsesAnIsolatedDatabase() throws Exception {
        try (Connection connection = dataSource.getConnection();
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("select current_database()")) {

            assertThat(resultSet.next()).isTrue();
            assertThat(resultSet.getString(1)).isEqualTo("patrimonio_test");
        }
    }

    @Test
    void hibernateValidaLosMapeosConDdlAutoValidate() {
        assertThat(environment.getProperty("spring.jpa.hibernate.ddl-auto"))
            .isEqualTo("validate");
        assertThat(environment.getProperty("spring.jpa.generate-ddl", Boolean.class))
            .isFalse();
        assertThat(entityManagerFactory.isOpen()).isTrue();

        Integer columnas = jdbcTemplate.queryForObject("""
            select count(*)
            from information_schema.columns
            where table_schema = 'public'
              and table_name = 'cuenta_financiera'
            """, Integer.class);

        assertThat(columnas)
            .as("el esquema de prueba debe contener las 10 columnas mapeadas")
            .isEqualTo(10);
    }
}
