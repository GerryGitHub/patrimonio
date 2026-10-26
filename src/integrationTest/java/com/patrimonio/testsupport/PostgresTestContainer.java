package com.patrimonio.testsupport;

import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Fábrica del contenedor PostgreSQL 16 efímero que usan las pruebas de
 * integración.
 *
 * <p>Cada clase de prueba crea su propio contenedor para aislar los datos y el
 * esquema. El contenedor aplica {@code db/esquema.sql}, publicado en el
 * classpath del source set {@code integrationTest}. En ningún caso se conecta a
 * la base de desarrollo ni al contenedor {@code patrimonio-postgres}.</p>
 */
public final class PostgresTestContainer {

    private static final String IMAGEN = "postgres:16";
    private static final String BASE_DE_DATOS = "patrimonio_test";
    private static final String USUARIO = "patrimonio_test";
    private static final String CONTRASENA = "patrimonio_test";

    private PostgresTestContainer() {
    }

    public static PostgreSQLContainer nuevo() {
        return new PostgreSQLContainer(DockerImageName.parse(IMAGEN))
            .withDatabaseName(BASE_DE_DATOS)
            .withUsername(USUARIO)
            .withPassword(CONTRASENA)
            .withInitScript("esquema.sql");
    }
}
