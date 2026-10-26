# Patrimonio — Estrategia de pruebas

> Documento de diseño. **No contiene código Java/Kotlin ejecutable, no crea el
> build y no modifica la base de datos.** Fecha: 2026-10-23.
>
> Esta estrategia se aplicará cuando exista el backend. Hoy no hay código de
> producción, build ni suites de prueba; el esquema PostgreSQL sí existe y es la
> referencia para las pruebas de persistencia.

## 1. Propósito y alcance

Definir **qué se prueba, en qué nivel, con qué herramientas y con qué garantías
de aislamiento**, para que la arquitectura hexagonal y los límites de módulos no
se degraden al implementar.

### 1.1 Dentro del alcance

- Pirámide de pruebas y responsabilidad de cada nivel.
- Aislamiento estricto de la base de datos de desarrollo.
- Pruebas de dominio sin Spring.
- Pruebas de casos de uso con puertos simulados.
- Pruebas del adaptador REST.
- Pruebas de persistencia contra PostgreSQL 16 real.
- Pruebas de integración de flujos críticos.
- Pruebas de arquitectura y límites entre módulos.
- Datos de prueba, cobertura, CI y criterios de calidad.

### 1.2 Fuera del alcance

- Pruebas de rendimiento, carga o estrés (no hay requisito ni herramienta en el
  stack).
- Pruebas de seguridad ofensiva o de penetración.
- Pruebas de interfaz de usuario (no hay frontend).
- Pruebas de contrato con consumidores externos (Pact u similar).
- Implementación de las pruebas: este documento no crea archivos de test.
- Cambios de esquema, datos, Docker Compose, contenedores o volúmenes.

## 2. Principios

1. **Probar comportamiento, no implementación.** Se verifica el resultado
   observable y las invariantes, no los detalles internos ni los getters.
2. **La base de datos de desarrollo es intocable.** Ninguna prueba se conecta a
   la base `patrimonio` del contenedor `patrimonio-postgres` ni al volumen
   `patrimonio_pgdata`.
3. **Pirámide:** muchas pruebas rápidas de dominio y casos de uso, menos pruebas
   de adaptador y muy pocas de integración completa.
4. **Determinismo:** tiempo inyectado (`Clock.fixed`), IDs explícitos cuando la
   aserción lo requiera, sin `Thread.sleep`, sin dependencia del orden.
5. **PostgreSQL real para persistencia.** No se usa H2 ni otra base embebida:
   no reproducen índices únicos parciales, CHECK, `timestamptz`, `numeric`,
   `gen_random_uuid()` ni el bloqueo de filas.
6. **El esquema de prueba sale de `db/esquema.sql`.** No se mantiene un segundo
   esquema escrito a mano ni se usa `create-drop`.
7. **Un fallo, una causa.** Cada prueba debe indicar qué regla se rompió.
8. **Rápido por defecto.** Docker solo donde aporta valor real; el resto corre
   en milisegundos.
9. **Los límites se verifican.** Las reglas de módulos se prueban con ArchUnit o
   con una checklist equivalente; un límite no verificado se rompe.
10. **La cobertura es una señal, no un objetivo.** Un 100 % sin aserciones no
    vale nada.

## 3. Punto de partida

| Elemento | Estado |
|----------|--------|
| Código de producción | No existe |
| Build Gradle | No existe |
| Suites de prueba | No existen |
| Base de datos | PostgreSQL 16, base `patrimonio`, 20 tablas, 0 filas |
| Migraciones | Ninguna (sin Flyway ni Liquibase) |
| DDL de referencia | `db/esquema.sql` |
| Propuestas v1/v2 | **No aplicadas**; no se prueban como si existieran |

La estrategia es **objetivo**, no una descripción de algo ya implementado.

## 4. Niveles de prueba

| Nivel | Qué verifica | Contexto Spring | Base de datos | Velocidad | Ubicación |
|-------|--------------|-----------------|---------------|-----------|-----------|
| Dominio | Invariantes, value objects, transiciones de estado | No | No | Milisegundos | `<módulo>/domain` |
| Casos de uso | Orquestación, puertos, errores, escrituras | No | No (fakes) | Milisegundos | `<módulo>/application` |
| Contrato de puerto | Que el fake y el adaptador real cumplen el mismo contrato | Parcial | Opcional | Milisegundos/segundos | `testsupport` + adaptador |
| Adaptador REST | HTTP, validación de forma, DTOs, `ProblemDetail` | `@WebMvcTest` | No | Rápido | `<módulo>/adapter/in/web` |
| Persistencia | Mapeo, restricciones, precisión, bloqueos | `@DataJpaTest` | PostgreSQL 16 efímero | Medio | `<módulo>/adapter/out/persistence` |
| Esquema | Que el DDL aplicado y el mapeo coinciden | Contexto con `validate` | PostgreSQL 16 efímero | Medio | `architecture`/`persistence` |
| Integración | Flujo completo por HTTP y estado persistido | `@SpringBootTest` | PostgreSQL 16 efímero | Lento | `<módulo>/integration` |
| Arquitectura | Dependencias y límites entre módulos | No | No | Rápido | `architecture` |

### 4.1 Forma de la pirámide

```text
                    ▲
                   /  \        Integración (pocas, flujos críticos)
                  /----\
                 / REST \      Adaptador REST (por recurso)
                /--------\
               /Persist. \    Persistencia + esquema (por módulo)
              /------------\
             / Casos de uso \ Puertos simulados, sin Spring
            /----------------\
           /     Dominio      \ Invariantes, Java puro
          /____________________\
```

La base debe ser amplia y rápida; la cima, pequeña y costosa. Si una regla se
puede probar en dominio, no se prueba en integración.

## 5. Herramientas y versiones

Versiones verificadas en fuentes oficiales el **2026-10-23**. Las administradas
por el BOM de Spring Boot **no se fijan a mano**.

| Herramienta | Versión | Origen / uso |
|-------------|---------|--------------|
| JUnit Jupiter | **6.0.3** | BOM de Boot 4.1.2; API `org.junit.jupiter.api` |
| AssertJ | **3.27.7** | BOM; aserciones fluidas |
| Mockito | **5.23.0** | BOM; `mockito-core` y `mockito-junit-jupiter` |
| Spring Boot Test | **4.1.2** | `spring-boot-starter-test` |
| Spring Test | **7.0.10** | BOM; contexto de pruebas |
| Testcontainers | **2.0.5** | BOM; `testcontainers`, `testcontainers-junit-jupiter`, `testcontainers-postgresql` |
| `spring-boot-testcontainers` | **4.1.2** | BOM; `@ServiceConnection` |
| PostgreSQL | **16** (`postgres:16`) | Misma imagen que `docker-compose.yml` |
| Driver JDBC | **42.7.14** | BOM |
| Awaitility | Administrada | Incluida en `spring-boot-starter-test`; esperas acotadas |
| Gradle | **9.8.2** | Wrapper; ejecución de tareas |
| ArchUnit | **1.5.1** (opcional) | No está en el BOM; fijar versión y verificar Java 25 |
| JaCoCo | **0.8.15** (opcional) | No está en el BOM; fijar versión y verificar Java 25 |

Las versiones de las dependencias administradas son informativas: el build las
hereda del BOM y **no** las fija a mano. Solo ArchUnit y JaCoCo, si se adoptan,
requieren una versión explícita.

### 5.1 Lo que no se usa

| Herramienta | Motivo |
|-------------|--------|
| H2 / HSQLDB / Derby | No reproducen el comportamiento real de PostgreSQL |
| TestNG / Spock | El stack usa JUnit Jupiter |
| `@MockBean` | No existe en Spring Boot 4; se usa `@MockitoBean` |
| Pact / contract testing | No hay consumidores externos; los módulos comparten proceso |
| Gatling / JMeter | Pruebas de carga fuera de alcance |
| Flyway / Liquibase | El esquema se administra fuera de la aplicación |
| `create-drop` / `update` | Prohibidos; destruyen o alteran el esquema |

### 5.2 JUnit 5 y JUnit 6

La pila solicitada menciona **JUnit 5**. El BOM de Boot 4.1.2 administra
**JUnit Jupiter 6.0.3**, que mantiene el paquete `org.junit.jupiter.api` y el
modelo de programación de Jupiter, pero es una generación mayor. Se recomienda
**aceptar la versión administrada** y documentar la desviación; forzar JUnit 5.x
obligaría a sobrescribir el BOM y a justificar compatibilidad con Spring Boot 4.
La decisión queda pendiente de aprobación (ver §19).

### 5.3 Paquetes de anotaciones en Spring Boot 4

Spring Boot 4 reorganizó las anotaciones de prueba por módulo. Las rutas
correctas son:

| Anotación | Paquete en Boot 4.1.2 |
|-----------|------------------------|
| `@WebMvcTest` | `org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest` |
| `@AutoConfigureMockMvc` | `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc` |
| `@DataJpaTest` | `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest` |
| `@AutoConfigureTestDatabase` | `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase` |
| `@SpringBootTest` | `org.springframework.boot.test.context.SpringBootTest` (sin cambio) |
| `@MockitoBean` | `org.springframework.test.context.bean.override.mockito.MockitoBean` |
| `@ServiceConnection` | `org.springframework.boot.testcontainers.service.connection.ServiceConnection` |
| `@Sql` | `org.springframework.test.context.jdbc.Sql` |

Usar las rutas antiguas de Boot 3 no compila en Boot 4.

### 5.4 Dependencias de prueba en Spring Boot 4

Spring Boot 4 separó las anotaciones de *slice* en módulos propios.
`spring-boot-starter-test` **no** incluye todos esos módulos; hay que declararlos
en el ámbito de prueba. Las versiones las administra el BOM.

| Dependencia | Ámbito | Proporciona |
|-------------|--------|-------------|
| `spring-boot-starter-test` | `test` | JUnit Jupiter, Mockito, AssertJ, Hamcrest, Awaitility y Spring Test |
| `spring-boot-webmvc-test` | `test` | `@WebMvcTest`, `@AutoConfigureMockMvc` |
| `spring-boot-data-jpa-test` | `test` | `@DataJpaTest`, `TestEntityManager`; incluye `spring-boot-jdbc-test` |
| `spring-boot-jdbc-test` | `test` | `@AutoConfigureTestDatabase` (transitiva con Data JPA; declararla solo si se usa sin JPA) |
| `spring-boot-testcontainers` | `test` | `@ServiceConnection` |
| `testcontainers-junit-jupiter` | `test` | `@Testcontainers`, `@Container` |
| `testcontainers-postgresql` | `test` | `PostgreSQLContainer` |
| `postgresql` | `runtime` | Driver JDBC; administrado por el BOM |

`spring-boot-starter-data-jpa` es una dependencia de producción, no de prueba.
Si una prueba de slice no compila porque no encuentra la anotación, el problema
es que falta el módulo de prueba correspondiente, no que haya que añadir
`spring-boot-test-autoconfigure` a mano.

## 6. Aislamiento de la base de datos de desarrollo

### 6.1 Regla dura

**Ninguna prueba, tarea de Gradle, script de CI o comando manual de la suite se
conecta a la base `patrimonio` de desarrollo, a su contenedor ni a su volumen.**

La base de desarrollo es la que se inspecciona con `psql` y se describe en
`docs/schema-review.md`; no es un entorno de pruebas. Un test que la use puede
borrar o alterar datos y, además, oculta el comportamiento real del esquema.

### 6.2 Mecanismos de aislamiento

| Mecanismo | Regla |
|-----------|-------|
| Testcontainers | Contenedor PostgreSQL 16 efímero por ejecución; sin volumen nombrado |
| Nombre de base | `patrimonio_test`, nunca `patrimonio` |
| Puerto | Aleatorio asignado por Docker; nunca el 5432 del desarrollo |
| Credenciales | Propias del contenedor (generadas o de prueba); no se leen de `.env` |
| Datasource | Lo aporta `@ServiceConnection`; no se declara una URL de desarrollo en `application-test.yml` |
| Reutilización | `withReuse(false)` por defecto; no se reutiliza el contenedor de desarrollo |
| Perfil | `test`; configuración sin secretos y sin URL fija |
| CI | Sin credenciales ni conectividad hacia la base de desarrollo |
| Docker Compose | Las pruebas **no** ejecutan `docker compose`, ni `down -v`, ni tocan `patrimonio_pgdata` |

### 6.3 Guarda de seguridad en las pruebas

Toda prueba que use base de datos debe verificar, antes de ejecutar lógica, que:

1. `SELECT current_database()` devuelve `patrimonio_test`.
2. La conexión apunta a un contenedor efímero, no a una URL configurada
   manualmente hacia `localhost:5432/patrimonio`.

Si la comprobación falla, la prueba **aborta**; no se salta ni se marca como
omitida. Esta guarda es barata y evita el accidente más caro.

### 6.4 Esquema de prueba

El esquema se carga una sola vez por contenedor, desde el DDL real:

1. Gradle copia `db/esquema.sql` a los recursos de prueba
   (`build/resources/test/db/esquema.sql`) antes de ejecutar las pruebas.
2. La copia se verifica por checksum contra el archivo fuente; si difieren, el
   build falla.
3. Testcontainers inicializa PostgreSQL con esa copia mediante un script de
   inicialización.
4. El contexto arranca con `spring.jpa.hibernate.ddl-auto=validate`.

Ventajas: una sola fuente de verdad, sin duplicar el esquema en el repositorio y
sin depender de rutas del host (problemáticas en Windows con Docker Desktop).

Alternativas y compromisos:

| Alternativa | Ventaja | Inconveniente |
|-------------|---------|---------------|
| Copia por Gradle + `withInitScript` | Portable, sin duplicado versionado, checksum | Requiere una tarea de copia |
| Montar `db/esquema.sql` con `MountableFile` | Usa el archivo directamente | Depende de rutas y de compartir unidad en Docker Desktop |
| Copia manual en `src/test/resources` | Simple | Se desincroniza con el DDL real |
| `create-drop` | Cómodo | **Prohibido**: no prueba el esquema real |

No se reejecuta el DDL para “limpiar” entre pruebas: eso oculta dependencias de
estado y es lento. La limpieza es de datos, no de esquema.

### 6.5 Limpieza de datos

| Nivel | Estrategia |
|-------|------------|
| `@DataJpaTest` | Rollback automático de la transacción de prueba |
| Casos de uso con fakes | Estado en memoria por prueba; se descarta |
| Integración | Datos únicos por prueba + limpieza dirigida en `@AfterEach` |
| Pruebas de bloqueo | Datos confirmados (commit) y limpieza posterior; no rollback |
| Pruebas de esquema | Transacción con rollback o contenedor nuevo |

Reglas:

- La limpieza respeta el orden de FK (`ON DELETE RESTRICT`): primero hijos,
  luego padres.
- `TRUNCATE` solo se permite dentro del contenedor efímero y con la guarda de
  §6.3; nunca contra la base de desarrollo.
- No se usa `@DirtiesContext` como mecanismo para “resetear la base”: reinicia
  el contexto, no los datos, y ralentiza la suite.
- Las pruebas no dependen del orden de ejecución ni de datos dejados por otra
  prueba.

### 6.6 Paralelismo

- Cada fork de Gradle obtiene su propio contenedor; no se comparte un contenedor
  entre procesos.
- Dentro de un fork, la ejecución paralela de JUnit se mantiene **desactivada**
  en la primera iteración.
- Si se activa, cada prueba usa datos únicos y no comparte estado mutable; los
  catálogos se insertan con identificadores propios.
- No se usan puertos fijos.

### 6.7 Docker

- Las pruebas de persistencia e integración **requieren Docker**.
- En CI, si Docker no está disponible, el job **falla**; no se omiten pruebas en
  silencio.
- En local, se puede excluir explícitamente el grupo `persistence`/`integration`
  para un ciclo rápido de unidad, pero la exclusión debe ser visible en el
  comando, no el comportamiento por defecto de `check`.
- Se usa la misma imagen `postgres:16` que el entorno de desarrollo para evitar
  diferencias de versión menor.

## 7. Pruebas de dominio

### 7.1 Características

- Java puro: JUnit Jupiter + AssertJ.
- **Sin** contexto Spring, sin Mockito, sin base de datos.
- Se instancian agregados y value objects reales.
- Se inyecta `Clock.fixed(...)`; no se usa `Instant.now()`.
- Se usan IDs explícitos cuando la aserción los necesita.

### 7.2 Qué se prueba

| Elemento | Ejemplos |
|----------|----------|
| Value objects | Moneda válida, importe con escala, cantidad no negativa, tasa en rango, rango de fechas coherente |
| Fábricas | `crear` y `reconstruir` aceptan datos válidos y rechazan inválidos |
| Invariantes | Una posición no puede quedar con cantidad negativa; un contrato no puede tener fecha de fin anterior al inicio |
| Transiciones | Registrar movimiento actualiza cantidad/precio; cerrar posición cambia estado; cambiar rol de membresía |
| Cálculos | Ganancia/pérdida, precio promedio, devengo simple/compuesto (cuando la fórmula se apruebe) |
| Errores | Excepción de dominio correcta, mensaje y código estables |

### 7.3 Qué no se prueba aquí

- Getters/setters triviales.
- Serialización JSON.
- Anotaciones JPA.
- Consultas SQL.
- Transacciones.
- Reglas que dependen de datos externos (eso es caso de uso o integración).

### 7.4 Ejemplos por módulo

| Módulo | Casos de dominio prioritarios |
|--------|-------------------------------|
| `catalogos` | Código de moneda con formato válido; decimales en rango; nombre de catálogo no vacío |
| `acceso` | Email válido; rol de membresía válido; usuario inactivo no puede operar (si se aprueba) |
| `cuentas` | Fechas de apertura/cierre coherentes; número de cuenta opcional; moneda obligatoria |
| `instrumentos` | Símbolo único por moneda (regla de aplicación, no solo DB); identificador con tipo y valor |
| `posiciones` | Cantidad y precio con escala; movimiento con signo según tipo (pendiente); cierre lógico |
| `contratos` | Tasa no negativa (positiva si el dominio lo exige); modalidad `SIMPLE`/`COMPUESTO`; vencimiento posterior al inicio; no-solape de tasas (si se valida en dominio) |
| `mercado` | Precio no negativo (positivo si el dominio lo exige); par de monedas distinto; tipo de cambio > 0 |
| `valoraciones` | Ganancia = valor de mercado − valor de costo; moneda coherente con la posición |

### 7.5 Estilo

- Un comportamiento por prueba; nombre que describa escenario y resultado.
- `@ParameterizedTest` para tablas de valores límite.
- `assertThatThrownBy` para errores; se verifica tipo y mensaje estable, no el
  stack trace.
- No se prueba la implementación privada; si una regla no se puede probar por la
  API pública, el diseño debe revisarse.

## 8. Pruebas de casos de uso

### 8.1 Características

- Se instancia el servicio de aplicación directamente, sin Spring.
- Los puertos de salida se sustituyen por **fakes** en memoria o por Mockito.
- Los puertos de consulta de otros módulos se simulan con DTOs.
- El dominio se usa real, no mockeado.
- `Clock.fixed` y comandos explícitos.

### 8.2 Fakes vs. Mockito

| Uso | Recomendación |
|-----|---------------|
| Repositorios de agregados | Fake en memoria; permite verificar estado y reutilizar en varias pruebas |
| Puertos de consulta entre módulos | Mockito o fake simple; se controla la respuesta |
| Verificar que no se guarda tras un error | Fake con contador de escrituras o `verify(..., never())` |
| Simular fallo de infraestructura | Mockito lanza la excepción del puerto |
| Dominio | Nunca mockear; usar objetos reales |

Los fakes viven en `src/test/java/.../testsupport` o en el paquete de pruebas del
módulo, no en producción.

### 8.3 Qué se prueba

- Camino feliz de cada caso de uso.
- Validaciones de negocio que dependen del estado (posición cerrada, contrato
  vencido, cuenta inexistente).
- Referencias entre módulos: se llama al puerto correcto con el ID correcto.
- Errores: no encontrado, conflicto, regla incumplida.
- **No escritura parcial:** si falla una validación, no se llama a `guardar`.
- Paginación y filtros en consultas.
- Idempotencia, si se define para pagos o importaciones.
- Resultado parcial en orquestaciones (por ejemplo, valorar portafolio).

### 8.4 Qué no se prueba aquí

- La transacción real y el rollback (eso es integración).
- El bloqueo pesimista real (eso es persistencia).
- El contrato HTTP (eso es el adaptador REST).
- El mapeo JPA (eso es persistencia).

### 8.5 Contrato de puerto

Para cada puerto de salida relevante se define una **suite de contrato** que se
ejecuta contra:

1. el fake en memoria, y
2. el adaptador real con PostgreSQL.

Así se evita que el fake “mienta” y que las pruebas de casos de uso pasen con un
comportamiento que la base no tiene. La suite cubre: búsqueda existente/ausente,
guardado, actualización, listados, paginación y errores esperados.

## 9. Pruebas del adaptador REST

### 9.1 Configuración

- `@WebMvcTest` del controlador concreto.
- `@AutoConfigureMockMvc`.
- `@MockitoBean` para el puerto de entrada.
- Se importan el `@RestControllerAdvice`, los assemblers y la configuración
  necesaria; no se importan repositorios ni entidades.
- Sin base de datos, sin Testcontainers.

### 9.2 Qué se prueba

| Caso | Resultado esperado |
|------|--------------------|
| Petición válida de creación | `201 Created`, `Location`, cuerpo con ID |
| Consulta existente | `200 OK`, DTO con campos esperados |
| Consulta inexistente | `404 Not Found`, `ProblemDetail` |
| DTO inválido | `400 Bad Request`, errores de campo |
| Conflicto de unicidad | `409 Conflict`, `code` estable |
| Referencia inválida | `409`/`422` según la decisión |
| Regla de negocio | `422 Unprocessable Entity` |
| Error inesperado | `500 Internal Server Error`, sin stack trace |
| Paginación fuera de rango | `400` o límite aplicado, según contrato |
| Respuesta | Nunca contiene campos de entidad JPA ni `hibernateLazyInitializer` |

### 9.3 Reglas

- Se verifica el **contrato JSON** (nombres de campos, tipos, nulos), no una
  cadena completa.
- Se verifica `Content-Type` y el formato `application/problem+json` en errores.
- No se prueba el framework ni la serialización de Jackson en sí.
- No se usa `@SpringBootTest` para probar solo un controlador.
- No se prueba seguridad todavía: no hay Spring Security en el stack. Cuando se
  decida, se añadirán filtros y pruebas con `spring-security-test`; no se
  inventa un flujo de autenticación ahora.

## 10. Pruebas de persistencia

### 10.1 Configuración

- `@DataJpaTest`.
- `@AutoConfigureTestDatabase(replace = NONE)` para que el slice no sustituya la
  conexión por una base embebida.
- Testcontainers PostgreSQL 16 + `@ServiceConnection` (o propiedades dinámicas
  como alternativa si el slice no procesa la conexión).
- `@Import` del adaptador y de los mappers que se prueban.
- `ddl-auto=validate`; esquema cargado desde `db/esquema.sql`.
- Rollback por defecto para las pruebas de mapeo.

### 10.2 Qué se prueba

| Área | Casos |
|------|-------|
| Mapeo | Las 20 tablas se leen/escriben con los tipos correctos |
| Precisión | `numeric(24,8)`, `numeric(28,10)`, `numeric(12,8)`, `numeric(20,10)` conservan escala |
| Fechas | `date` → `LocalDate`; `timestamptz` → `Instant` UTC; sin corrimiento |
| Enums | `membresia.rol` y `contrato_inversion.modalidad_interes` con `EnumType.STRING` |
| Nulos | Columnas opcionales y obligatorias según el esquema |
| IDs | UUID asignado por la aplicación; el `DEFAULT` de la base no se usa como mecanismo principal |
| UNIQUE | Incluidos los índices únicos parciales (`cuenta_financiera`, `instrumento`) |
| FK | `ON DELETE RESTRICT`: borrar un padre con hijos falla |
| CHECK | Formato de moneda, precios positivos, fechas coherentes, enums |
| Bloqueo | `PESSIMISTIC_WRITE` bloquea una segunda transacción y libera al commit/rollback |
| Traducción | `23505`, `23503`, `23514`, `23502` → excepción de dominio/aplicación |

### 10.3 Pruebas de bloqueo

El bloqueo pesimista **no se puede probar** con el rollback automático de
`@DataJpaTest` ni dentro de una única transacción. Se necesita:

1. Confirmar los datos de partida en una transacción.
2. Abrir dos transacciones en hilos distintos con conexiones distintas.
3. La primera adquiere el bloqueo y espera; la segunda intenta y queda en espera.
4. Se verifica el orden y el tiempo con `Awaitility`/`@Timeout`, no con
   `Thread.sleep`.
5. Se limpia después; el contenedor es efímero.

Estas pruebas son lentas y propensas a intermitencias: se limitan a `Posicion` y
`ContratoInversion`, que son los agregados con escritura concurrente.

### 10.4 Pruebas de esquema

Además de `validate`, se comprueban objetos concretos del catálogo:

- Existencia de las 20 tablas y de las columnas mapeadas.
- Existencia de las restricciones críticas por nombre (PK, FK, UNIQUE, CHECK).
- Existencia de los índices usados por las consultas principales.
- Comportamiento de los índices únicos parciales con valores NULL y no NULL.
- Comportamiento de `ON DELETE RESTRICT`.
- Comportamiento de los CHECK con valores límite.

Los conteos (20 tablas, 28 FK, 12 UNIQUE, 18 CHECK, 62 índices) pueden usarse
como **canario**, pero no como única aserción: un conteo igual no garantiza que
la restricción correcta exista. Se afirman nombres y comportamientos.

`validate` **no** comprueba índices, FK, UNIQUE, CHECK ni defaults; por eso las
pruebas anteriores son necesarias y no redundantes.

### 10.5 Lo que no se hace

- No se usa H2.
- No se usa `create`, `create-drop` ni `update`.
- No se ejecuta el DDL contra la base de desarrollo.
- No se prueba con datos de producción ni con dumps reales.
- No se añaden índices “porque la prueba va lenta”; un índice es un cambio de
  esquema y requiere propuesta.

## 11. Pruebas de integración

### 11.1 Configuración

- `@SpringBootTest(webEnvironment = RANDOM_PORT)`.
- Testcontainers PostgreSQL 16 + `@ServiceConnection`.
- Cliente HTTP real (`TestRestTemplate`/`RestClient`) o `MockMvc` con contexto
  completo, según lo que se quiera verificar.
- Sin `@Transactional` en la prueba, para observar commits reales.
- `@Timeout` para evitar pruebas colgadas.

### 11.2 Flujos críticos

| Flujo | Qué se verifica |
|-------|-----------------|
| Alta de portafolio → cuenta → instrumento → posición | Se persiste la cadena completa y las FK son válidas |
| Registro de movimiento | Se actualiza la posición y se inserta el movimiento en una transacción |
| Movimiento inválido | No queda movimiento ni cambio de posición (rollback) |
| Contrato → tasa → pago | Se respetan las reglas y las restricciones |
| Cotización → valoración | Se usa la cotización correcta y se guarda una valoración por posición/fecha |
| Conflicto de unicidad | La API devuelve `409` y no deja datos parciales |

No se duplican aquí todos los casos de dominio, casos de uso y REST: solo se
prueba que las piezas encajan y que la transacción real se comporta como se
espera.

### 11.3 Estado y limpieza

- Se usan datos únicos por prueba (UUID/sufijo) para evitar colisiones.
- La limpieza se hace en orden de FK y solo en el contenedor efímero.
- No se depende del orden de ejecución.
- No se usa `@DirtiesContext` salvo para un caso muy justificado.

## 12. Pruebas de arquitectura

### 12.1 ArchUnit (opcional, recomendado)

Se proponen las reglas A1–A10 de `module-boundaries.md`:

| # | Regla |
|---|-------|
| A1 | `..domain..` no depende de Spring, JPA, Hibernate, Jackson ni Jakarta Validation |
| A2 | `..adapter.in.web..` no depende de `..adapter.out.persistence..` ni de `..application.port.out..` |
| A3 | `..adapter.out.persistence..` no depende de `..adapter.in.web..` |
| A4 | Un módulo solo importa `..<otro>.api..`, no `domain`, `application` ni `adapter` ajenos |
| A5 | No hay ciclos entre paquetes de módulos |
| A6 | Los controladores solo llaman a `..application.port.in..` |
| A7 | `..api..` no depende de `..adapter..` ni de `..application.service..` |
| A8 | `shared` no depende de ningún módulo de negocio |
| A9 | Ningún `@Entity` fuera de `..adapter.out.persistence.entity..` |
| A10 | Ningún `@Transactional` en `..adapter.in.web..` ni en `..adapter.out.persistence..` |

Se ejecutan como pruebas unitarias, sin contexto Spring.

**Advertencia:** ArchUnit 1.5.1 es la última estable (2026-09-25), pero **no está
administrada por el BOM** y hay que verificar que soporte bytecode de Java 25
antes de adoptarla. Si no lo soporta, se usa una checklist de revisión de PR
equivalente; el límite sigue siendo obligatorio.

### 12.2 Reglas adicionales

- `..application.service..` solo depende de su dominio, sus puertos y
  `shared.application`.
- Los DTOs de `api` no tienen anotaciones JPA ni Jackson.
- No hay `BaseEntity`, `BaseRepository<T, ID>` ni `AbstractCrudService<T>`.
- Los repositorios Spring Data solo se usan dentro de
  `adapter.out.persistence`.

## 13. Datos y fixtures

| Regla | Detalle |
|-------|---------|
| Sin datos de producción | Nunca se copian filas reales, emails ni identificadores personales |
| Sin estado estático mutable | Cada prueba crea sus datos; no hay singletons compartidos |
| Builders/factorías | `TestDataFactory` por módulo, con valores por defecto válidos y overrides explícitos |
| Importes | `new BigDecimal("...")`, nunca `double` |
| Fechas | `LocalDate` fijos y `Clock.fixed`; no `now()` |
| IDs | Fijos solo cuando la aserción lo requiere; aleatorios en el resto |
| Catálogos | Mínimos; códigos únicos por prueba o fixture compartido con limpieza |
| SQL de prueba | Solo para restricciones/esquema; en `src/test/resources/sql`, explícito y revisado |
| PII | Prohibida en fixtures y logs |

No se crea un “seed” de producción para pruebas. Los catálogos reales se cargan
en su entorno con un proceso aprobado, no desde la suite.

## 14. Cobertura y criterios de calidad

### 14.1 JaCoCo (opcional)

- JaCoCo 0.8.15 es la última estable, pero no está en el BOM; fijar versión y
  verificar Java 25.
- Se genera informe HTML/XML y se publica como artefacto de CI.
- Se excluyen clases de configuración, `*Application`, entidades JPA, DTOs y
  código generado.
- La cobertura se mide por paquete/módulo, no solo global.

### 14.2 Umbrales propuestos

| Capa | Línea | Rama |
|------|-------|------|
| `domain` | ≥ 85 % | ≥ 75 % |
| `application` | ≥ 80 % | ≥ 70 % |
| `adapter.in.web` | ≥ 70 % | ≥ 60 % |
| `adapter.out.persistence` | ≥ 65 % | ≥ 50 % |
| Global | ≥ 75 % | — |

Son un punto de partida; se ajustan tras la primera línea base. No se bloquea el
primer commit por cobertura si aún no hay suites; a partir de la primera versión
funcional, los umbrales se vuelven obligatorios en CI.

### 14.3 Puertas de calidad

- Todas las pruebas de unidad, arquitectura, persistencia e integración en verde.
- Ninguna prueba de persistencia/integración omitida en CI.
- Sin reintentos automáticos que oculten intermitencias.
- Sin `Thread.sleep`; esperas con Awaitility o `@Timeout`.
- Sin pruebas dependientes del orden.
- Informe de cobertura publicado.
- Revisión de que no se añadió una dependencia no aprobada.

## 15. Integración continua

### 15.1 Etapas

| Etapa | Contenido | Docker | Duración objetivo |
|-------|-----------|--------|-------------------|
| Compilación | Compilar producción y pruebas | No | < 2 min |
| Unidad | Dominio, casos de uso, REST slice, arquitectura | No | < 5 min |
| Persistencia | `@DataJpaTest`, contrato de esquema | Sí | < 10 min |
| Integración | Flujos críticos con `@SpringBootTest` | Sí | < 15 min |
| Cobertura | Informe y verificación de umbrales | No | < 1 min |

### 15.2 Tareas

- `test`: unidad, REST slice y arquitectura; excluye persistencia e integración.
- `persistenceTest`: grupo `persistence`; requiere Docker.
- `integrationTest`: grupo `integration`; requiere Docker.
- `check`: depende de todas las anteriores.
- `jacocoTestReport` / `jacocoTestCoverageVerification`: cuando se adopte.

Los nombres son orientativos; lo importante es que **la suite rápida no dependa
de Docker** y que la suite con base de datos sea explícita.

### 15.3 Reglas de CI

- Java 25 (Temurin) y wrapper Gradle 9.8.2.
- Caché de Gradle y pre-descarga de la imagen `postgres:16`.
- Un fork por job al inicio; aumentar solo con evidencia de estabilidad.
- Sin secretos de desarrollo; credenciales del contenedor efímeras.
- Sin acceso de red a la base de desarrollo.
- Reportes JUnit XML y cobertura como artefactos.
- `maxParallelForks` conservador; contenedor por fork.
- Si Docker falla, el job falla con un mensaje claro; no se omiten pruebas.
- Las pruebas intermitentes se cuarentenan con un issue y se corrigen; no se
  reintentan indefinidamente.

### 15.4 Imagen de PostgreSQL

- Se usa `postgres:16`, la misma etiqueta que `docker-compose.yml`.
- `postgres:16` es una etiqueta flotante dentro de la serie 16; para
  reproducibilidad estricta se puede fijar un digest. Es una decisión pendiente:
  fijar digest mejora la reproducibilidad, pero hay que actualizarlo de forma
  consciente.

## 16. Matriz de pruebas por módulo

Prioridad: **A** = alta, **M** = media, **B** = baja.

| Módulo | Dominio | Casos de uso | REST | Persistencia | Integración | Foco |
|--------|---------|--------------|------|--------------|-------------|------|
| `catalogos` | B | M | M | A | B | UNIQUE, CHECK, formato de moneda, desactivación lógica |
| `acceso` | M | A | A | A | A | Email único, membresía única, roles, baja lógica |
| `cuentas` | M | A | A | A | A | FK, UNIQUE parcial de número, fechas |
| `instrumentos` | M | A | A | A | M | UNIQUE parcial de símbolo, identificadores |
| `posiciones` | A | A | A | A | A | Movimiento + actualización, bloqueo, FK, precisión |
| `contratos` | A | A | A | A | A | Tasas, no-solape (pendiente), pagos, vencimiento |
| `mercado` | M | M | M | A | M | UNIQUE de cotización y tipo de cambio, precios |
| `valoraciones` | A | A | M | A | A | UNIQUE posición/fecha, NULL, ganancia/pérdida |

La prioridad no implica que los módulos con **B** no se prueben: indica dónde se
concentra el esfuerzo y dónde un fallo tiene más impacto.

## 17. Organización y nomenclatura

### 17.1 Estructura

```text
src/test/java/com/patrimonio/
├── shared/
├── acceso/
│   ├── domain/
│   ├── application/
│   └── adapter/in/web/
├── cuentas/
├── instrumentos/
├── posiciones/
│   ├── domain/
│   ├── application/
│   └── adapter/out/persistence/
├── contratos/
├── mercado/
├── valoraciones/
├── testsupport/          # fakes, builders, fixtures compartidos
└── architecture/         # ArchUnit o checklist
```

Los paquetes de prueba reflejan los de producción. No hay pruebas en `src/main`.

### 17.2 Sufijos y etiquetas

| Tipo | Sufijo | Etiqueta JUnit |
|------|--------|----------------|
| Unidad de dominio/aplicación | `Test` | `unit` |
| Adaptador REST | `Test` | `web` |
| Persistencia | `Test` | `persistence` |
| Integración | `IT` | `integration` |
| Arquitectura | `Test` | `architecture` |

Las etiquetas permiten seleccionar suites sin depender del nombre del archivo.

### 17.3 Nombres de métodos

Se propone:

- Clases y código de producción en inglés.
- Nombres de pruebas en español, orientados al negocio:
  `registrarMovimiento_cuandoPosicionCerrada_falla`.
- Estructura Given/When/Then en el cuerpo, sin comentarios redundantes.

La decisión de idioma queda pendiente; lo importante es la consistencia.

## 18. Anti-patrones prohibidos

| Anti-patrón | Por qué se prohíbe |
|-------------|--------------------|
| Probar contra la base `patrimonio` de desarrollo | Riesgo de pérdida de datos y resultados no reproducibles |
| H2 en lugar de PostgreSQL | No reproduce el esquema real |
| `ddl-auto=create/create-drop/update` en pruebas | Destruye el esquema y oculta desalineaciones |
| `@Transactional` en pruebas de bloqueo | El rollback impide observar el bloqueo real |
| `Thread.sleep` | Pruebas lentas e intermitentes |
| Depender del orden de pruebas | Fallos ocultos y no reproducibles |
| Datos estáticos mutables compartidos | Contaminación entre pruebas |
| Mockear el dominio | Se prueba el mock, no la regla |
| Verificar getters/setters | No aporta comportamiento |
| `@MockBean` | No existe en Boot 4; usar `@MockitoBean` |
| `@SpringBootTest` para todo | Lentitud innecesaria |
| `@DirtiesContext` como reset de base | No limpia datos y reinicia contexto |
| Reintentos para “arreglar” intermitencias | Oculta defectos |
| Omitir pruebas de persistencia en CI | Falsa sensación de seguridad |
| Copiar un dump de producción | Riesgo de PII y datos obsoletos |
| Añadir índices para acelerar pruebas | Cambio de esquema no aprobado |

## 19. Riesgos y decisiones pendientes

| # | Tema | Riesgo | Decisión necesaria |
|---|------|--------|--------------------|
| 1 | JUnit 5 vs. JUnit 6.0.3 | La pila pide JUnit 5; el BOM administra 6 | Aceptar el BOM y documentar la desviación, o justificar una versión 5.x |
| 2 | ArchUnit | No está en el BOM; Java 25 | Aprobar 1.5.1 tras verificar bytecode, o usar checklist |
| 3 | JaCoCo | No está en el BOM; Java 25 | Aprobar 0.8.15 tras verificar soporte, o posponer cobertura |
| 4 | Docker en CI/local | Sin Docker no hay persistencia ni integración | Definir política: fallar siempre en CI; exclusión explícita en local |
| 5 | Etiqueta `postgres:16` | Flotante dentro de 16 | ¿Fijar digest para reproducibilidad? |
| 6 | Copia del esquema | Duplicado o desincronización | Aprobar copia por Gradle + checksum |
| 7 | Nombre de base de prueba | Conexión accidental a desarrollo | Aprobar `patrimonio_test` y la guarda de §6.3 |
| 8 | Umbrales de cobertura | Bloqueo prematuro o métrica vacía | Aprobar la tabla de §14.2 tras la línea base |
| 9 | Idioma de nombres de prueba | Inconsistencia | Aprobar español para tests, inglés para producción |
| 10 | Fakes vs. Mockito | Fakes infieles o mocks frágiles | Aprobar fakes para repositorios y Mockito para puertos de consulta |
| 11 | Paralelismo | Intermitencias y contención | Mantener secuencial en la primera iteración |
| 12 | Pruebas de bloqueo | Complejas y lentas | Limitar a `Posicion` y `ContratoInversion` |
| 13 | Seguridad | No hay Spring Security | Posponer pruebas de autenticación hasta decidir el stack |
| 14 | Contrato con consumidores | No hay consumidores externos | No adoptar Pact por ahora |
| 15 | Rendimiento | Sin herramienta ni requisito | Fuera de alcance; revisar `EXPLAIN` manualmente |
| 16 | `@ServiceConnection` en slices | Puede no aplicarse en todos los slices | Verificar en la primera implementación; usar propiedades dinámicas como respaldo |
| 17 | Limpieza de datos | `TRUNCATE` accidental | Guarda de §6.3 y limpieza dirigida |
| 18 | Permisos DDL en pruebas | El contenedor usa superusuario | Prueba opcional con rol DML para validar que la app no necesita DDL |

## 20. Referencias

- Spring Boot 4.1.2 — testing:
  <https://docs.spring.io/spring-boot/4.1.2/reference/testing/index.html>
- Spring Boot 4.1.2 — testing Spring Boot applications:
  <https://docs.spring.io/spring-boot/4.1.2/reference/testing/spring-boot-applications.html>
- Spring Boot 4.1.2 — `@WebMvcTest`:
  <https://docs.spring.io/spring-boot/4.1.2/api/java/org/springframework/boot/webmvc/test/autoconfigure/WebMvcTest.html>
- Spring Boot 4.1.2 — `@DataJpaTest`:
  <https://docs.spring.io/spring-boot/4.1.2/api/java/org/springframework/boot/data/jpa/test/autoconfigure/DataJpaTest.html>
- Spring Boot 4.1.2 — `@AutoConfigureTestDatabase`:
  <https://docs.spring.io/spring-boot/4.1.2/api/java/org/springframework/boot/jdbc/test/autoconfigure/AutoConfigureTestDatabase.html>
- Spring Boot 4.1.2 — `@ServiceConnection`:
  <https://docs.spring.io/spring-boot/4.1.2/api/java/org/springframework/boot/testcontainers/service/connection/ServiceConnection.html>
- Spring Framework 7.0.10 — testing:
  <https://docs.spring.io/spring-framework/reference/testing.html>
- Spring Framework 7.0.10 — `@MockitoBean`:
  <https://docs.spring.io/spring-framework/docs/7.0.10/javadoc-api/org/springframework/test/context/bean/override/mockito/MockitoBean.html>
- JUnit 6.0.3 — user guide:
  <https://docs.junit.org/6.0.3/user-guide/index.html>
- Testcontainers — guía de Spring Boot:
  <https://testcontainers.com/guides/testing-spring-boot-rest-api-using-testcontainers/>
- ArchUnit — user guide:
  <https://www.archunit.org/userguide/html/000_Index.html>
- JaCoCo — documentación:
  <https://www.jacoco.org/jacoco/trunk/doc/>
- Esquema aplicado: `db/esquema.sql`
- Revisión del esquema: `docs/schema-review.md`
- Propuesta v2 (no aplicada): `docs/schema-change-proposal-v2.md`

## 21. Documentos relacionados

| Documento | Contenido |
|-----------|-----------|
| `architecture-overview.md` | Resumen, stack, decisiones y mapa de módulos |
| `hexagonal-architecture.md` | Capas, puertos, adaptadores, mapeo y transacciones |
| `module-boundaries.md` | Módulos, propiedad de tablas, dependencias y reglas R1–R16 |
| `persistence-strategy.md` | `ddl-auto`, mapeo, repositorios, restricciones y brechas |
