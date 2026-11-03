# Patrimonio — Módulo `cuentas`: consulta de cuentas financieras

> Documento de las fases 2 y 3.1. Fecha: 2026-10-26.
>
> Implementa la consulta de solo lectura de las cuentas financieras de un
> portafolio. La fase 3.1 añade el bloqueo de exposición fuera de desarrollo
> local, el enmascaramiento del número de cuenta y la normalización de errores
> HTTP. No crea, edita ni elimina datos, no modifica el esquema PostgreSQL y no
> implementa autenticación.

## 1. Endpoint

```http
GET /api/v1/portafolios/{portafolioId}/cuentas
```

- `portafolioId` es un UUID.
- Respuesta correcta: `200 OK` con un array JSON (vacío si el portafolio existe
  pero no tiene cuentas).
- Solo se devuelven las cuentas cuyo `cuenta_financiera.portafolio_id` coincide
  con el identificador de la ruta.
- El orden es determinista: `nombre ASC, id ASC`.
- El endpoint solo existe si el perfil `local` o `test` está activo de forma
  **explícita** y `patrimonio.api.cuentas.expuesta=true`. Un arranque sin perfil
  no lo registra, aunque Spring aplique `spring.profiles.default=local` y la
  propiedad valga `true` en el perfil local. En `prod` responde `404` y el
  arranque falla si se intenta habilitar.

Ejemplo:

```json
[
  {
    "id": "4f4c1a7e-3a5b-4c2e-9f10-1a2b3c4d5e6f",
    "nombre": "Cuenta corriente",
    "numeroCuenta": "****1332",
    "institucionId": "0b6f1f2a-1111-4a2b-8c3d-1234567890ab",
    "tipoCuentaId": "1c7a2b3c-2222-4b3c-9d4e-234567890abc",
    "monedaId": "2d8b3c4d-3333-4c4d-ae5f-34567890abcd",
    "activo": true,
    "fechaApertura": "2024-01-15",
    "fechaCierre": null
  }
]
```

### 1.1 Campos de la respuesta

| Campo | Tipo | Nulo | Origen |
|-------|------|------|--------|
| `id` | UUID | No | `cuenta_financiera.id` |
| `nombre` | string | No | `cuenta_financiera.nombre` |
| `numeroCuenta` | string enmascarado | Sí | `cuenta_financiera.numero_cuenta` |
| `institucionId` | UUID | No | `cuenta_financiera.institucion_id` |
| `tipoCuentaId` | UUID | No | `cuenta_financiera.tipo_cuenta_id` |
| `monedaId` | UUID | No | `cuenta_financiera.moneda_id` |
| `activo` | boolean | No | `cuenta_financiera.activo` |
| `fechaApertura` | date (`YYYY-MM-DD`) | Sí | `cuenta_financiera.fecha_apertura` |
| `fechaCierre` | date (`YYYY-MM-DD`) | Sí | `cuenta_financiera.fecha_cierre` |

No se devuelve `portafolioId` porque ya forma parte de la ruta. Tampoco se
devuelven nombres de institución, tipo de cuenta o moneda: esta fase no hace
joins con otros módulos y expone solo los identificadores. No se devuelven
contraseñas, credenciales, datos de usuario ni columnas que no existan en el
esquema.

`numeroCuenta` **nunca viaja completo por HTTP**. El valor íntegro se conserva
en el dominio y en la persistencia para uso interno; la respuesta solo incluye
la máscara descrita en la sección 5.

### 1.2 Errores HTTP

| Situación | HTTP | `code` | Detalle |
|-----------|------|--------|---------|
| `portafolioId` no es un UUID válido | `400` | `ID_INVALIDO` | El parámetro no tiene formato válido |
| Cuerpo o validación de entrada inválidos | `400` | `VALIDACION_ENTRADA` | La solicitud no cumple el formato esperado |
| El portafolio no existe | `404` | `NO_ENCONTRADO` | No existe el portafolio indicado |
| El dominio rechaza la consulta | `422` | `CUENTA_INVALIDA` | Mensaje de invariante, sin datos sensibles |
| Los datos almacenados no permiten reconstruir la cuenta | `500` | `DATOS_INCONSISTENTES` | Mensaje genérico; el detalle interno no se expone |
| Error inesperado | `500` | `ERROR_INTERNO` | Mensaje genérico; sin traza ni SQL |
| Método, `Accept` o `Content-Type` no soportados | `405`, `406`, `415` | — | Los resuelve Spring MVC sin detalles internos |

Los errores se serializan como RFC 9457 `application/problem+json` mediante
`ProblemDetail`. El `code` es estable y el `detail` no contiene nombres de
tablas, SQL, entidades JPA, trazas ni valores financieros. El campo `instance`
incluye únicamente la ruta solicitada.

Los detalles técnicos se registran en el log del servidor. El manejador de
`DatosPersistidosInvalidos` registra la traza completa, pero los mensajes de las
excepciones de dominio no incluyen el número de cuenta ni otros valores
sensibles. El manejador genérico también registra la traza y devuelve un
mensaje fijo; no se capturan `Error` ni se devuelven excepciones de Spring tal
cual.

## 2. Arquitectura

El módulo sigue la estructura de `docs/architecture/hexagonal-architecture.md`:

```text
com.patrimonio
├── shared
│   └── domain/error
│       ├── DomainException
│       ├── NotFoundException
│       └── DatosPersistidosInvalidos
├── acceso
│   ├── api
│   │   ├── dto/PortafolioResumen
│   │   └── port/PortafolioQueryPort
│   └── adapter/out/persistence
│       ├── entity/PortafolioEntity
│       ├── repository/PortafolioJpaRepository
│       └── adapter/PortafolioQueryAdapter
└── cuentas
    ├── domain
    │   ├── model/CuentaFinanciera
    │   ├── model/NumeroCuenta
    │   └── error/CuentaInvalidaException
    ├── application
    │   ├── port/in/ListarCuentasPorPortafolioUseCase
    │   ├── port/out/CuentaFinancieraRepository
    │   ├── query/ListarCuentasPorPortafolioQuery
    │   ├── view/CuentaFinancieraView
    │   └── service/ListarCuentasPorPortafolioService
    └── adapter
        ├── in/web
        │   ├── CuentaFinancieraController
        │   ├── CuentaFinancieraResponse
        │   ├── CuentaFinancieraResponseAssembler
        │   ├── EnmascaradorNumeroCuenta
        │   ├── CuentasApiExposicionGuard
        │   └── CuentaFinancieraExceptionHandler
        └── out/persistence
            ├── entity/CuentaFinancieraEntity
            ├── repository/CuentaFinancieraJpaRepository
            ├── mapper/CuentaFinancieraMapper
            └── adapter/CuentaFinancieraRepositoryAdapter
```

Flujo:

```text
GET /api/v1/portafolios/{id}/cuentas
  → CuentaFinancieraController
  → ListarCuentasPorPortafolioUseCase
  → PortafolioQueryPort (acceso.api)        [existe el portafolio]
  → CuentaFinancieraRepository (puerto out)
  → CuentaFinancieraRepositoryAdapter
  → CuentaFinancieraJpaRepository
  → CuentaFinancieraMapper (entidad → dominio)
  → CuentaFinancieraView
  → CuentaFinancieraResponseAssembler (enmascara el número)
  → JSON
```

Decisiones:

- El dominio no importa Spring, JPA, Hibernate, Jackson ni Jakarta Validation.
- El caso de uso declara `@Transactional(readOnly = true)` y es el único límite
  transaccional. El controlador y el adaptador de persistencia no abren
  transacciones.
- `cuentas` no accede directamente a la tabla `portafolio`: usa el contrato
  público `PortafolioQueryPort` del módulo `acceso`, cuya implementación vive en
  `acceso.adapter.out.persistence`.
- La entidad JPA nunca se serializa ni se pasa al caso de uso.
- La exposición del endpoint se decide en el adaptador web con una propiedad
  explícita. La autorización real no vive en el dominio ni se simula con el
  UUID del portafolio.

## 3. Persistencia

### 3.1 Mapeo explícito

`CuentaFinancieraEntity` mapea la tabla real `public.cuenta_financiera`:

| Columna PostgreSQL | Tipo | Campo Java | Anotación |
|--------------------|------|------------|-----------|
| `id` | `uuid` PK | `UUID id` | `@Id`, sin `@GeneratedValue` |
| `portafolio_id` | `uuid` NOT NULL FK | `UUID portafolioId` | `@Column` |
| `institucion_id` | `uuid` NOT NULL FK | `UUID institucionId` | `@Column` |
| `tipo_cuenta_id` | `uuid` NOT NULL FK | `UUID tipoCuentaId` | `@Column` |
| `nombre` | `varchar(150)` NOT NULL | `String nombre` | `@Column(length = 150)` |
| `numero_cuenta` | `varchar(64)` NULL | `String numeroCuenta` | `@Column(length = 64)` |
| `moneda_id` | `uuid` NOT NULL FK | `UUID monedaId` | `@Column` |
| `activo` | `boolean` NOT NULL | `boolean activo` | `@Column` |
| `fecha_apertura` | `date` NULL | `LocalDate fechaApertura` | `@Column` |
| `fecha_cierre` | `date` NULL | `LocalDate fechaCierre` | `@Column` |

No hay asociaciones JPA, ni `@ManyToOne`, ni carga perezosa. Las referencias se
mantienen por UUID, tal como están en el esquema. No se inventan columnas ni
relaciones.

`PortafolioEntity` mapea `public.portafolio` solo para la comprobación de
existencia. La consulta proyecta `id`, `nombre`, `moneda_base_id` y `activo`
directamente a `PortafolioResumen`; no se carga el agregado completo.

### 3.2 Reglas de consulta

- `CuentaFinancieraJpaRepository.listarPorPortafolio` usa JPQL y filtra por
  `portafolio_id`; no existe `findAll()` sin filtro.
- La consulta selecciona las columnas del agregado porque la respuesta las
  necesita; no se cargan instituciones, tipos de cuenta ni monedas.
- El adaptador se une a la transacción de solo lectura del caso de uso.
- `open-in-view` está desactivado; el mapeo ocurre dentro de la transacción.
- No se pagina en esta fase: el número de cuentas por portafolio es acotado y el
  requisito no define paginación. Si el producto la necesita, se añadirá
  `PageQuery`/`PageResult` sin cambiar el contrato actual de campos.

### 3.3 Esquema y Hibernate

- `ddl-auto=validate` en `local` y `test`; `none` en `prod`.
- Hibernate no crea, actualiza ni elimina tablas.
- No se usa Flyway ni Liquibase.
- Las pruebas de integración aplican `db/esquema.sql` tal cual en un PostgreSQL
  16 efímero de Testcontainers. No se ejecuta DDL ni DML contra la base de
  desarrollo.

## 4. Reglas de dominio

`CuentaFinanciera.reconstruir(...)` valida:

- `id`, `portafolioId`, `institucionId`, `tipoCuentaId` y `monedaId` no nulos.
- `nombre` no vacío y de máximo 150 caracteres.
- `numeroCuenta`, cuando existe, no vacío y de máximo 64 caracteres.
- Si existen `fechaApertura` y `fechaCierre`, `fechaCierre >= fechaApertura`.
  Esta regla replica el CHECK `ck_cuenta_financiera_fechas`.

`numeroCuenta`, `fechaApertura` y `fechaCierre` se representan en el dominio como
`Optional`. El esquema no impone una relación entre `activo` y `fechaCierre`; no
se inventa esa regla en esta fase.

Si una fila almacenada viola una invariante, el mapper lanza
`DatosPersistidosInvalidos` (`DATOS_INCONSISTENTES`) y la API responde `500` sin
exponer el detalle. No se corrige el dato en silencio.

Se devuelven todas las cuentas del portafolio, estén activas o no. El requisito
no define un filtro por `activo`.

## 5. Enmascaramiento del número de cuenta

El valor completo se conserva en `NumeroCuenta`, en la entidad JPA y en la base
de datos. El enmascaramiento ocurre solo en `CuentaFinancieraResponseAssembler`,
en el borde HTTP:

| Valor almacenado | Valor devuelto |
|------------------|----------------|
| `ES91 2100 0418 4502 0005 1332` | `****1332` |
| `12345` | `****2345` |
| `1234` | `****` |
| `123` | `***` |
| `null`, `""` o solo espacios | `null` |

Reglas:

- Se ignoran los espacios exteriores antes de enmascarar.
- Si quedan cuatro caracteres o menos, se ocultan todos.
- Si quedan más de cuatro, se muestran solo los cuatro últimos.
- No se lanza ninguna excepción por valores nulos, vacíos, en blanco o cortos.
- El número completo no se registra en logs ni aparece en mensajes de error.
- No existe un endpoint para revelar el número completo. Se añadirá solo cuando
  exista autorización explícita y una decisión de producto sobre qué rol puede
  verlo.

## 6. Seguridad

**La autenticación y la autorización definitivas están pendientes.** El endpoint
es únicamente para desarrollo local y pruebas. **No es apto para exposición
pública ni para producción.**

Situación actual:

- No hay Spring Security ni filtro de autenticación.
- No se identifica al usuario que llama.
- No se comprueba que el usuario pertenezca al portafolio solicitado.
- Conocer un UUID **no** constituye autorización.
- Cualquier cliente que pueda alcanzar el servicio en `local` o `test` puede
  consultar las cuentas de cualquier `portafolioId` que conozca o adivine.
- El esquema tiene `usuario` y `membresia`, pero el backend todavía no
  implementa identidad ni autorización. No se crean usuarios, roles ni
  membresías ficticios.

Protección existente:

1. `patrimonio.api.cuentas.expuesta` vale `false` por defecto en
   `application.yml` y en `application-prod.yml`.
2. `application-local.yml` y `application-test.yml` activan la propiedad.
3. `CuentaFinancieraController` usa la condición `PerfilDeDesarrolloExplicito`,
   que solo lo registra si `local` o `test` aparece en
   `Environment.getActiveProfiles()`, y además exige
   `@ConditionalOnProperty(expuesta=true)`. El perfil por defecto `local` no
   cuenta: sin perfil explícito el endpoint responde `404` aunque la propiedad
   valga `true`.
4. `CuentasApiExposicionGuard` falla el arranque si el perfil `prod` está activo
   y la propiedad vale `true`. No basta con cambiar la propiedad para exponer el
   endpoint en producción.
5. `server.error.include-message`, `include-stacktrace` e `include-exception`
   están desactivados.
6. El número de cuenta se enmascara y no se registra completo.

Para habilitar el endpoint en desarrollo local hay que activar el perfil
`local` de forma explícita:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
# o bien
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

Arrancar sin perfil deja el endpoint deshabilitado (`404`), aunque el perfil
por defecto sea `local`. En `prod` no se debe habilitar nunca.

Estas medidas evitan una exposición accidental, pero **no sustituyen** la
autorización. Antes de exponer el endpoint a usuarios reales se requiere, como
mínimo:

1. Autenticar al usuario y obtener su identidad de forma fiable.
2. Verificar la membresía en `membresia` para el portafolio solicitado y exigir
   un rol con permiso de lectura (por ejemplo, `LECTOR` o superior).
3. Decidir si un usuario sin membresía recibe `403` o `404` para no revelar la
   existencia del portafolio.
4. Sustituir el bloqueo por perfil/propiedad por la configuración de seguridad
   real; no habilitar `expuesta=true` en `prod` como atajo.
5. Añadir pruebas de seguridad con `spring-security-test` cuando se adopte
   Spring Security.
6. Diseñar un endpoint de revelación del número completo solo con autorización
   explícita y auditoría.

## 7. Pruebas

### 7.1 Unitarias (`src/test`, sin Spring ni Docker)

| Clase | Qué verifica |
|-------|--------------|
| `CuentaFinancieraTest` | Invariantes y valores opcionales del agregado |
| `NumeroCuentaTest` | Validación del value object |
| `ListarCuentasPorPortafolioServiceTest` | Camino feliz, lista vacía, 404, propagación de errores y mapeo a vista |
| `CuentaFinancieraMapperTest` | Mapeo entidad → dominio y traducción de datos incoherentes |
| `CuentaFinancieraControllerTest` | Contrato JSON, máscara, `200`, `400`, `404`, `422`, `500` y ausencia de datos internos |
| `EnmascaradorNumeroCuentaTest` | Números largos, cortos, vacíos, nulos y espacios exteriores |
| `CuentasApiExposicionGuardTest` | Arranque bloqueado en `prod` con la API habilitada |
| `CuentaFinancieraApiNoExpuestaTest` | El controlador no se registra y la ruta responde `404` en `prod` |
| `CuentaFinancieraApiSinPerfilTest` | Sin perfil explícito el controlador no se registra y la ruta responde `404`, incluso con `expuesta=true` |
| `CuentaFinancieraApiLocalTest` | Con perfil `local` explícito y `expuesta=true`, el controlador se registra y responde `200` |
| `HexagonalArchitectureTest` | Reglas de dependencia entre dominio, aplicación, adaptadores, módulos y `shared` |

`CuentaFinancieraControllerTest` usa `@WebMvcTest` (slice web) con el puerto de
entrada mockeado; no arranca JPA, no usa base de datos y no requiere Docker.
`CuentaFinancieraApiSinPerfilTest`, `CuentaFinancieraApiLocalTest` y
`CuentaFinancieraApiNoExpuestaTest` también usan `@WebMvcTest`, pero no mockean
el controlador: comprueban con el contexto real si el bean
`CuentaFinancieraController` está registrado y si la ruta responde o no.

`HexagonalArchitectureTest` usa la API core de ArchUnit 1.5.1 dentro de pruebas
JUnit normales. Se evitó el motor `archunit-junit6` porque depende de JUnit
Platform 6.1.x y el BOM de Spring Boot 4.1.2 administra JUnit 6.0.3; la API core
evita mezclar versiones y ArchUnit 1.5.1 reconoce class files de Java 25 (hasta
Java 27). Las reglas comprobadas son:

- El dominio solo depende de `java..` y `com.patrimonio..` (sin Spring, JPA,
  Hibernate, Jackson ni Jakarta Validation).
- La aplicación no depende de los adaptadores.
- El adaptador web no depende de persistencia, de puertos de salida, de
  `org.springframework.data..` ni de `jakarta.persistence..`.
- El adaptador de persistencia no depende del adaptador web.
- `cuentas` solo importa la API pública de `acceso`, nunca sus capas internas.
- `acceso` no depende de `cuentas`.
- La API pública no depende de adaptadores ni de servicios de aplicación.
- `shared` no depende de módulos de negocio.
- No hay ciclos entre los módulos `acceso`, `cuentas` y `shared`.
- Los controladores dependen del puerto de entrada.
- Los adaptadores de salida implementan contratos internos (puertos de salida o
  API pública).

### 7.2 Integración (`src/integrationTest`, PostgreSQL 16 efímero)

| Clase | Qué verifica |
|-------|--------------|
| `PatrimonioApplicationIT` | El contexto arranca con `ddl-auto=validate`, la base es `patrimonio_test` y `cuenta_financiera` tiene las 10 columnas mapeadas |
| `CuentaFinancieraRepositoryAdapterIT` | Filtro por portafolio, mapeo de columnas, opcionales, orden, datos incoherentes y consulta de existencia de portafolio |
| `CuentaFinancieraApiIT` | Flujo HTTP completo: filtro por portafolio, cuentas ajenas, máscara de números largos y cortos, lista vacía, `400`, `404`, `500`, mapeo fila → entidad → DTO y limpieza de datos |

Las pruebas de integración usan Testcontainers con `postgres:16` y aplican
`db/esquema.sql` con `withInitScript("esquema.sql")`. Cada clase crea su propio
contenedor efímero; los datos se insertan con UUID propios y se limpian en orden
de clave foránea dentro del contenedor. Nunca se usa la base de desarrollo ni el
contenedor `patrimonio-postgres`.

`CuentaFinancieraApiIT` no mockea puertos: arranca el contexto completo con
`@SpringBootTest`, ejecuta el controlador con `@AutoConfigureMockMvc` y verifica
que el JSON coincide con las filas insertadas. También comprueba que la entidad
JPA leída por el repositorio conserva el número completo, mientras la respuesta
HTTP solo devuelve la máscara.

### 7.3 Cobertura

El proyecto aplica el plugin JaCoCo con la versión 0.8.16, que soporta
oficialmente Java 25. Se generan tres informes:

| Tarea | Informe |
|-------|---------|
| `jacocoTestReport` | Unitarias: `build/reports/jacoco/test/html/index.html` |
| `jacocoIntegrationTestReport` | Integración: `build/reports/jacoco/integrationTest/html/index.html` |
| `jacocoMergedReport` | Combinado: `build/reports/jacoco/merged/html/index.html` |

No se impone un umbral mínimo: el informe es informativo en esta fase.

### 7.4 Comandos

```bash
./gradlew compileJava
./gradlew test                          # unitarias y arquitectura, sin Docker
./gradlew integrationTest               # integración con PostgreSQL 16 efímero
./gradlew check                         # ambas suites
./gradlew jacocoTestReport              # cobertura de unitarias
./gradlew jacocoIntegrationTestReport   # cobertura de integración
./gradlew jacocoMergedReport            # cobertura combinada
```

En Windows, `gradlew.bat`. Requiere Java 25 y Docker para `integrationTest`.

### 7.5 Resultados de la última ejecución

Ejecutado el 2026-10-28 con Java 25.0.5+8 y Docker 29.4.0, siempre con
`--no-daemon`:

| Comando | Resultado |
|---------|-----------|
| `compileJava` | `BUILD SUCCESSFUL` |
| `test` | 64 aprobadas, 0 fallidas, 0 omitidas |
| `integrationTest` | 13 aprobadas, 0 fallidas, 0 omitidas (`BUILD SUCCESSFUL` en 49 s) |
| `check --rerun-tasks` | `BUILD SUCCESSFUL` en 2 m 25 s |
| `jacocoTestReport` | `BUILD SUCCESSFUL`; línea 182/213 (85.4%), rama 35/52 (67.3%) |
| `jacocoMergedReport` | línea 195/213 (91.5%), rama 35/52 (67.3%) |

No quedaron pruebas pendientes de ejecución. Las pruebas de integración usaron
exclusivamente Testcontainers; no se conectaron a la base de desarrollo.

Una ejecución anterior de `integrationTest` quedó bloqueada dentro de un daemon
de Gradle y se descartó sin producir resultados. Las ejecuciones definitivas se
hicieron con `--no-daemon`; no se reutilizó ningún resultado de la ejecución
bloqueada.

## 8. Límites de esta fase

- Solo consulta; no hay alta, edición, cierre ni borrado de cuentas.
- No se implementan instituciones, catálogos, posiciones ni otros módulos.
- No se implementa autenticación, autorización, usuarios ni membresías.
- No se modifica el esquema, los datos de desarrollo, `docker-compose.yml` ni
  sus volúmenes.
- No se usan Flyway, Liquibase ni `ddl-auto=update`.
- No se hacen joins con `institucion`, `tipo_cuenta` ni `moneda`.
- No se devuelven entidades JPA.
- No se añaden abstracciones genéricas de repositorios o paginación.
- No se devuelve el número de cuenta completo ni se añade un endpoint para
  revelarlo.
