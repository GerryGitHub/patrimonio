# Patrimonio — Bootstrap del backend

> Documento de infraestructura de la Fase 1. Fecha: 2026-10-26.
>
> La Fase 2 añade el módulo de negocio `cuentas` y sus entidades JPA; el detalle
> está en [`cuentas-financieras.md`](cuentas-financieras.md). Este documento
> describe el bootstrap: no se modifica el esquema PostgreSQL, no se toca
> `docker-compose.yml` ni sus volúmenes y no se añaden herramientas de
> migración.

## 1. Stack y versiones

| Componente | Versión | Origen de la versión |
| ---------- | ------- | -------------------- |
| Java | 25 (Temurin 25.0.5+8, LTS) | `java.toolchain` en `build.gradle.kts` |
| Spring Boot | 4.1.2 | plugin `org.springframework.boot` y BOM |
| Gradle | 9.8.2 | wrapper (`gradle/wrapper/gradle-wrapper.properties`) |
| Spring Framework | 7.0.10 | BOM de Spring Boot |
| Spring Web MVC | `spring-boot-starter-webmvc` 4.1.2 | BOM de Spring Boot |
| Spring Data JPA | `spring-boot-starter-data-jpa` 4.1.2 | BOM de Spring Boot |
| Hibernate ORM | 7.4.13.Final | BOM de Spring Boot (no se fija versión) |
| HikariCP | 7.0.2 | BOM de Spring Boot |
| PostgreSQL | 16 | imagen `postgres:16` en Testcontainers |
| Driver JDBC PostgreSQL | 42.7.14 | BOM de Spring Boot |
| Jakarta Bean Validation | 3.1.1 / Hibernate Validator 9.1.5.Final | BOM de Spring Boot |
| JUnit Jupiter | 6.0.3 | BOM de Spring Boot |
| Mockito | 5.23.0 | BOM de Spring Boot |
| AssertJ | 3.27.7 | BOM de Spring Boot |
| Testcontainers | 2.0.5 | BOM de Spring Boot |

Comprobación de compatibilidad (2026-10-26):

- Spring Boot 4.1.2 es la última versión estable publicada (2026-10-22).
- La documentación oficial de requisitos del sistema de Spring Boot 4.1.2
  indica Java 17–27 y Gradle 8.14+ o 9.x; Java 25 está dentro del rango.
- Todas las versiones de librerías se resuelven con el BOM
  `org.springframework.boot:spring-boot-dependencies:4.1.2`. El build solo
  declara explícitamente la versión de Spring Boot.

## 2. Estructura

```
patrimonio/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/wrapper/
│   ├── gradle-wrapper.jar
│   └── gradle-wrapper.properties
└── src/
    ├── main/
    │   ├── java/com/patrimonio/PatrimonioApplication.java
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml
    │       ├── application-prod.yml
    │       └── application-test.yml
    ├── test/
    │   └── java/com/patrimonio/PatrimonioApplicationTest.java
    └── integrationTest/
        └── java/com/patrimonio/PatrimonioApplicationIT.java
```

- `src/test` contiene pruebas rápidas: las de dominio, aplicación y mapper no
  arrancan Spring; la de contrato REST usa un slice `@WebMvcTest` sin base de
  datos. Ninguna necesita Docker.
- `src/integrationTest` es un *source set* independiente. Sus pruebas usan
  Testcontainers y se ejecutan con la tarea `integrationTest`.
- La Fase 2 añade los paquetes `com.patrimonio.cuentas`,
  `com.patrimonio.acceso.api`/`adapter` y `com.patrimonio.shared.domain.error`.
  El detalle está en [`cuentas-financieras.md`](cuentas-financieras.md).

## 3. Perfiles de Spring

| Perfil | Uso | `ddl-auto` | Datasource |
| ------ | --- | ---------- | ---------- |
| `local` (por defecto) | Desarrollo en la máquina | `validate` | PostgreSQL de desarrollo vía variables de entorno |
| `test` | Pruebas de integración | `validate` | Inyectado por Testcontainers (`@ServiceConnection`) |
| `prod` | Producción | `none` | PostgreSQL vía variables de entorno |

`application.yml` define los valores comunes:

- `spring.profiles.default: local`
- `spring.jpa.open-in-view: false`
- `spring.jpa.generate-ddl: false`
- `spring.jpa.hibernate.ddl-auto: none` como valor base seguro
- `spring.sql.init.mode: never` (sin scripts de inicialización)

`application-local.yml` y `application-test.yml` sobrescriben `ddl-auto` a
`validate`. `application-prod.yml` mantiene `none`.

### Por qué `validate` en local y `none` en producción

- **Local / test:** `validate` comprueba que el mapeo JPA coincide con el
  esquema existente y falla al arrancar si no coincide. No crea ni modifica
  tablas. Es una red de seguridad durante el desarrollo.
- **Producción:** `none`. Hibernate no inspecciona ni toca el esquema. El
  esquema es la fuente de verdad y los cambios se aplican manualmente mediante
  propuestas revisadas (ver `docs/architecture/persistence-strategy.md`).

No se usa Flyway, Liquibase ni ningún equivalente.

## 4. Variables de entorno

El backend lee la configuración de PostgreSQL de estas variables:

| Variable | Descripción | Valor por defecto en `local` |
| -------- | ----------- | ---------------------------- |
| `POSTGRES_HOST` | Host de PostgreSQL | `localhost` |
| `POSTGRES_PORT` | Puerto | `5432` |
| `POSTGRES_DB` | Base de datos | `patrimonio` |
| `POSTGRES_USER` | Usuario | `patrimonio` |
| `POSTGRES_PASSWORD` | Contraseña | **sin valor por defecto** (obligatoria) |
| `SERVER_PORT` | Puerto HTTP | `8080` |

Las credenciales reales no se versionan. En el perfil `local`,
`application-local.yml` importa opcionalmente el `.env` de la raíz como archivo
de propiedades:

```yaml
spring:
  config:
    import: optional:file:.env[.properties]
```

Así, `Run` en el IDE y `./gradlew bootRun` reutilizan las mismas credenciales
que Docker Compose sin configuración adicional. Si `.env` no existe, la
aplicación sigue arrancando y usa las variables de entorno del sistema (o los
valores por defecto). Las variables de entorno del sistema tienen prioridad
sobre `.env`.

En perfiles distintos de `local` (por ejemplo `prod`) `.env` no se importa:
define las variables en el entorno del proceso. Ejemplo en PowerShell:

```powershell
$env:POSTGRES_HOST = "localhost"
$env:POSTGRES_PORT = "5432"
$env:POSTGRES_DB = "patrimonio"
$env:POSTGRES_USER = "patrimonio"
$env:POSTGRES_PASSWORD = "tu_contrasena_local"
```

## 5. Pruebas

### 5.1 Unitarias (`src/test`)

- Se ejecutan con JUnit 6 y AssertJ.
- Las pruebas de dominio, aplicación y mapper no arrancan el contexto de Spring.
- La prueba del contrato REST usa `@WebMvcTest` (slice web) con el puerto de
  entrada mockeado; no abre conexiones ni usa JPA.
- Ninguna prueba unitaria requiere Docker ni PostgreSQL.
- La Fase 2 añade pruebas para `CuentaFinanciera`, `NumeroCuenta`, el caso de
  uso, el mapper y el controlador de cuentas.
- La Fase 3 añade `HexagonalArchitectureTest`, que aplica reglas de dependencia
  con la API core de ArchUnit 1.5.1 dentro de pruebas JUnit normales. Se usa la
  API core en lugar de `archunit-junit6` porque el motor de ArchUnit depende de
  JUnit Platform 6.1.x y el BOM de Boot 4.1.2 administra JUnit 6.0.3.
- El detalle está en [`cuentas-financieras.md`](cuentas-financieras.md).

### 5.2 Integración (`src/integrationTest`)

- `PatrimonioApplicationIT` arranca el contexto completo con
  `@SpringBootTest` y el perfil `test`.
- `@Testcontainers` inicia un contenedor efímero `postgres:16` con base
  `patrimonio_test` y credenciales exclusivas de prueba.
- `@ServiceConnection` conecta el `DataSource` de Spring al contenedor; no se
  usa la base de desarrollo.
- El esquema se aplica con `withInitScript("esquema.sql")`; `db/` se publica en
  el classpath del source set `integrationTest`.
- `PatrimonioApplicationIT` comprueba que el contexto arranca con
  `ddl-auto=validate`, que la base es `patrimonio_test` y que la tabla
  `cuenta_financiera` contiene las 10 columnas mapeadas.
- `CuentaFinancieraRepositoryAdapterIT` verifica el mapeo y el filtro por
  portafolio contra el esquema real.
- `CuentaFinancieraApiIT` ejecuta el flujo HTTP completo con `MockMvc` y el
  contexto real: filtro por portafolio, cuentas ajenas, lista vacía, `400`,
  `404`, `500`, y mapeo fila → entidad JPA → DTO. Los datos se crean y limpian
  dentro del contenedor.
- `PostgresTestContainer` centraliza la configuración del contenedor; cada clase
  de prueba crea el suyo para aislar datos.
- El contenedor se destruye al terminar; no persiste datos ni volúmenes del
  proyecto.

Requisito: Docker en marcha para las pruebas de integración. Las unitarias no lo
necesitan. Si Docker no está disponible, `integrationTest` falla y no se debe
reportar como aprobado.

## 6. Comandos Gradle

Todos los comandos se ejecutan desde la raíz del repositorio. En Windows usa
`gradlew.bat`; en Linux/macOS usa `./gradlew`. El wrapper descarga Gradle 9.8.2
la primera vez.

```bash
# Compilar el código de producción
./gradlew compileJava

# Compilar todo (producción, unitarias e integración) sin ejecutar pruebas
./gradlew classes testClasses integrationTestClasses

# Pruebas unitarias y de arquitectura (no requieren Docker ni base de datos)
./gradlew test

# Pruebas de integración con PostgreSQL 16 aislado (requieren Docker)
./gradlew integrationTest

# Build completo: compila, ejecuta unitarias, arquitectura e integración y genera el jar
./gradlew build

# Cobertura JaCoCo
./gradlew jacocoTestReport
./gradlew jacocoIntegrationTestReport
./gradlew jacocoMergedReport

# Limpiar artefactos
./gradlew clean
```

`check` depende de `integrationTest`, por lo que `build` ejecuta ambas suites.
Si solo se quieren unitarias, usar `./gradlew test`.

Los informes de cobertura quedan en `build/reports/jacoco/{test,integrationTest,merged}/html/index.html`.
JaCoCo se configura con la versión 0.8.16, que soporta oficialmente Java 25. No
se impone un umbral mínimo.

Si `JAVA_HOME` no apunta a un JDK 25, Gradle falla al resolver el *toolchain*.
Configúralo antes de ejecutar, por ejemplo en PowerShell:

```powershell
$env:JAVA_HOME = "C:\ruta\al\jdk-25"
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
```

## 7. Dependencias declaradas

Producción:

- `org.springframework.boot:spring-boot-starter-webmvc`
- `org.springframework.boot:spring-boot-starter-data-jpa`
- `org.springframework.boot:spring-boot-starter-validation`
- `org.postgresql:postgresql` (`runtimeOnly`)

Pruebas:

- `org.springframework.boot:spring-boot-starter-test`
- `org.springframework.boot:spring-boot-webmvc-test`
- `org.springframework.boot:spring-boot-data-jpa-test`
- `org.springframework.boot:spring-boot-testcontainers`
- `org.testcontainers:testcontainers-junit-jupiter`
- `org.testcontainers:testcontainers-postgresql`
- `com.tngtech.archunit:archunit:1.5.1` (única dependencia de prueba con versión
  explícita, porque no la administra el BOM de Boot)

El resto de dependencias de prueba no fija versión: provienen del BOM de Spring
Boot 4.1.2. JaCoCo es un plugin de Gradle, no una dependencia de la aplicación.

## 8. Límites de esta fase

- La Fase 1 no incluía módulos de negocio. La Fase 2 añade un único módulo de
  solo lectura, `cuentas`; no hay altas, ediciones ni borrados.
- No se ha modificado el esquema ni los datos de PostgreSQL.
- No se ha modificado `docker-compose.yml` ni sus volúmenes.
- No se han añadido Flyway, Liquibase ni scripts de inicialización.
- El dominio es Java puro: sin Spring, JPA, Hibernate, Jackson ni anotaciones de
  Jakarta. La persistencia vive en adaptadores de salida.
- La autenticación y la autorización están pendientes; el endpoint de cuentas es
  solo para desarrollo local. Ver
  [`cuentas-financieras.md`](cuentas-financieras.md).
