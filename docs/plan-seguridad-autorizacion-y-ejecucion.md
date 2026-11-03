# Patrimonio - Plan de autenticación, autorización, `execute()` y Strategy

> **Documento de planificación. No implementado.**
> Fecha: 2026-10-28.
> Estado: propuesta para revisión y autorización.
>
> Este documento **no modifica** código de aplicación, `db/esquema.sql`,
> configuración operativa, base de datos, contenedores ni historial de Git.
> No se ha ejecutado ninguna prueba durante su elaboración.
>
> Referencia histórica: commit `23c3b6d`. El estado real del repositorio
> incluye cambios locales sin commit de la fase 3.1 y de la corrección de
> perfiles; este plan los conserva y no asume que `23c3b6d` sea el estado
> implementado.

---

## 1. Objetivo y alcance

Definir cómo integrar **progresivamente**, sin reconstruir lo existente:

1. **Autenticación** verificable de usuarios.
2. **Autorización por membresía** en el portafolio solicitado.
3. Un método canónico **`execute(...)`** en los puertos de entrada.
4. El uso de **Strategy** solo donde exista una variación real, evitando
   abstracciones genéricas.

El alcance inmediato es la documentación. La implementación queda bloqueada
hasta autorización explícita. El endpoint de cuentas seguirá sin exponerse a
usuarios reales mientras no estén completas la autenticación, la autorización y
sus pruebas.

---

## 2. Línea base verificada

### 2.1 Repositorio

| Elemento | Estado verificado |
|---|---|
| Rama | `main`, `main...origin/main` |
| HEAD | `23c3b6d feat: implementar backend de cuentas financieras` |
| Árbol de trabajo | 22 entradas sin commit antes de este documento (14 modificadas y 8 nuevas); 23 al añadirlo |
| `db/esquema.sql` | Sin cambios locales |
| `docker-compose.yml`, `.env`, `.env.example` | Sin cambios locales |
| Contenedor `patrimonio-postgres` | Creado el 2026-10-23, `Up 4 days (healthy)`; no se ha tocado |
| Dependencias de seguridad | No existen en `build.gradle.kts` |

Los cambios locales incluyen la fase 3.1 (exposición condicionada, guarda de
producción, enmascaramiento, contrato de errores, pruebas y documentación) y la
corrección del perfil por defecto (`PerfilDeDesarrolloExplicito`, pruebas sin
perfil y con perfil local). **No se han modificado ni descartado.**

### 2.2 Qué existe y funciona

| Área | Implementación real | Evidencia |
|---|---|---|
| Endpoint de cuentas | `GET /api/v1/portafolios/{portafolioId}/cuentas` | `CuentaFinancieraController` |
| Puerto de entrada | `ListarCuentasPorPortafolioUseCase.consultar(Query)` | `application/port/in` |
| Servicio | `ListarCuentasPorPortafolioService`, `@Transactional(readOnly = true)` | `application/service` |
| Puerto de salida | `CuentaFinancieraRepository.listarPorPortafolio(UUID)` | `application/port/out` |
| Persistencia | Entidad JPA, repositorio Spring Data, mapper y adaptador | `adapter/out/persistence` |
| Dominio | `CuentaFinanciera`, `NumeroCuenta` | `domain/model` |
| Portafolio | `PortafolioQueryPort.buscarPorId(UUID)` y `PortafolioResumen` | `acceso/api` |
| Enmascaramiento | `****` + últimos 4; valores cortos ocultos | `EnmascaradorNumeroCuenta` |
| Errores | `ProblemDetail` con códigos estables | `CuentaFinancieraExceptionHandler` |
| Exposición | Perfil `local`/`test` explícito + propiedad `expuesta=true` | `PerfilDeDesarrolloExplicito`, `@ConditionalOnProperty` |
| Producción | Arranque falla con `prod` + `expuesta=true` | `CuentasApiExposicionGuard` |
| Pruebas | 64 unitarias y 13 de integración en la última ejecución registrada | `build/test-results` |

La última ejecución registrada es del **2026-10-28**: 64 pruebas unitarias y 13
de integración, sin fallos ni omisiones; cobertura unitaria 182/213 líneas
(85,4 %) y combinada 195/213 (91,5 %). **No se han vuelto a ejecutar en esta
fase de planificación.**

### 2.3 Qué no existe

- No hay Spring Security ni ningún filtro de autenticación.
- No hay `UsuarioEntity`, `MembresiaEntity`, `UsuarioRepository` ni
  `MembresiaRepository`.
- No hay `AccesoQueryPort` ni `PermisoPortafolio`, aunque la arquitectura los
  propone.
- No hay `RolMembresia`, `CapacidadPortafolio` ni política de roles.
- No hay usuario autenticado, principal, contraseñas hasheadas en uso ni
  pruebas de seguridad.
- No hay método `execute(...)`; el puerto usa `consultar(...)`.
- No hay patrón Strategy explícito. `EnmascaradorNumeroCuenta` es una clase
  concreta con un único algoritmo.
- No hay alta de usuarios, alta de membresías ni endpoint de login.
- No hay auditoría persistida ni tabla de sesiones/tokens.

---

## 3. Inventario: completo, parcial, pendiente y no verificado

| Funcionalidad | Estado | Detalle |
|---|---|---|
| Consulta de cuentas por portafolio | **Completa** | Contrato, dominio, persistencia, máscara, errores y pruebas |
| Exposición local/pruebas y bloqueo en producción | **Completa** | Condición de perfil explícito, propiedad y guarda de arranque |
| Contrato de errores de `cuentas` | **Completa** | 400/404/422/500 con `ProblemDetail`; sin detalles internos |
| `acceso` como consulta de portafolios | **Parcial** | Solo `PortafolioQueryPort`; no hay usuarios ni membresías |
| Autenticación | **Pendiente** | Solo existe `usuario.password_hash` en el esquema |
| Autorización por membresía | **Pendiente** | Solo existe la tabla `membresia` y su CHECK de roles |
| `execute(...)` | **Pendiente** | El código usa `consultar(...)`; la documentación usa `ejecutar`/`consultar` |
| Strategy | **Pendiente de decisión** | No hay variación implementada que lo justifique todavía |
| Pruebas de seguridad | **Pendiente** | No hay `spring-security-test` ni contexto autenticado |
| Exposición en producción | **No verificada** | La guarda existe; no se ha probado un despliegue real con TLS |
| Alta de usuarios y membresías | **Pendiente** | No hay casos de uso ni endpoints |
| Auditoría de accesos | **Pendiente** | Solo logs; no hay tabla de auditoría |

---

## 4. Discrepancias entre documentación y código

Se registran ambas versiones. **El código verificado es la referencia del
estado implementado.**

| # | Documentación | Código real | Tratamiento |
|---|---|---|---|
| 1 | `hexagonal-architecture.md:110` propone `ejecutar(Command)` para escrituras y `consultar(Query)` para lecturas | `ListarCuentasPorPortafolioUseCase.consultar(...)`; no existe `execute` ni `ejecutar` | El plan propone unificar en `execute(...)`; se actualizará la documentación solo tras autorización |
| 2 | `module-boundaries.md:51` y `hexagonal-architecture.md:265` declaran `AccesoQueryPort` y `PermisoPortafolio` | Solo existe `PortafolioQueryPort` y `PortafolioResumen` | Se planifica añadirlos; no se simulan |
| 3 | `architecture-overview.md:43` excluye Spring Security del stack | No hay dependencia de seguridad | El plan requiere aprobar explícitamente la incorporación de Spring Security |
| 4 | `hexagonal-architecture.md:286` deja `PasswordHasher` pendiente | No existe `PasswordHasher` | Se propone `PasswordEncoder` de Spring Security; no se crea un hash propio |
| 5 | `testing-strategy.md:438` dice que no se prueba seguridad | No hay pruebas de seguridad | Coherente; se añadirán en la fase correspondiente |
| 6 | `cuentas-financieras.md:266` y `backend-bootstrap.md:310` dicen que autenticación/autorización están pendientes | No hay autenticación ni autorización | Coherente; el plan no las declara implementadas |
| 7 | `module-boundaries.md:53` dice que las reglas de rol no están definidas | `membresia.rol` solo tiene CHECK de valores | El plan propone una matriz; queda pendiente de aprobación de producto |
| 8 | `CuentaFinancieraApiSinPerfilTest` espera `404` sin perfil | Hoy es correcto porque no hay filtros de seguridad | Con seguridad global, una petición anónima puede recibir `401` antes del routing; se ajustará la prueba y se documentará el cambio |
| 9 | `architecture-overview.md` describe `AccesoQueryPort` como API pública | No está implementado | Se reutiliza el nombre y el contrato propuesto, no se inventa otro |

---

## 5. Decisiones de diseño

### 5.1 Principios

1. **Reutilizar antes de crear.** Se usan las tablas `usuario`, `portafolio` y
   `membresia`, los puertos existentes y el contrato de errores.
2. **La autenticación es un adaptador de entrada.** Spring Security vive en
   `acceso.adapter.in.security`; el dominio y la aplicación no dependen de él.
3. **La autorización de objeto es de aplicación.** La comprobación de membresía
   se hace en el caso de uso, no en el controlador ni en el dominio.
4. **El dominio sigue puro.** Sin Spring Security, sin `SecurityContextHolder`,
   sin JPA.
5. **Sin segunda implementación.** No se crea otro controlador de cuentas, otro
   caso de uso ni otro modelo de portafolio.
6. **Sin abstracciones genéricas.** No hay `UseCase<I, O>`, `CommandBus`,
   `BaseService` ni `GenericRepository`.
7. **Seguridad por defecto.** Si falta un permiso, se deniega. Si falta
   autenticación, `401`. Si no hay membresía, no se revela la existencia del
   portafolio.

### 5.2 Autenticación

**Decisión propuesta:** Spring Security con **HTTP Basic sobre TLS** en el
primer incremento, sin sesiones y sin tokens.

Motivos:

- El esquema ya tiene `usuario.email`, `usuario.password_hash` y
  `usuario.activo`; no hace falta una tabla de tokens.
- No hay frontend ni consumidor móvil que exija JWT/OIDC todavía.
- HTTP Basic es verificable con `MockMvc`, `TestRestTemplate` y
  `spring-security-test` sin inventar un flujo de login.
- El punto de variación queda en `AuthenticationProvider`: añadir OIDC/JWT más
  adelante no cambia los casos de uso ni la autorización por membresía.

Reglas:

| Tema | Decisión |
|---|---|
| Almacenamiento | `usuario.password_hash`; nunca contraseña en claro |
| Hash | `PasswordEncoder` con BCrypt; coste por defecto, configurable |
| Identidad | `usuario.id` (UUID); el email es solo el nombre de usuario |
| Email | Normalizado (trim + minúsculas) al alta y al buscar |
| Usuario inactivo | `isEnabled() = false`; la autenticación falla con `401` genérico |
| Autoridades globales | Solo `ROLE_USUARIO`; los roles de portafolio **no** son globales |
| Sesión | `STATELESS`; sin `JSESSIONID` |
| CSRF | Deshabilitado mientras no haya cookies; se reactivará si se usa sesión |
| Transporte | TLS obligatorio; Basic sin TLS no es aceptable |
| Logs | Nunca `Authorization`, contraseña, hash ni token |
| Errores | `401` genérico; no se revela si el email existe |
| Fuerza bruta, MFA, recuperación | Fuera de alcance de este plan |

El principal autenticado será un `UsuarioDetails` que implementa
`UserDetails` y el contrato público `UsuarioAutenticado` (`id`, `email`,
`nombre`). El controlador recibe `@AuthenticationPrincipal UsuarioAutenticado`
y **nunca** acepta `usuarioId` como parámetro HTTP.

**Alternativa rechazada:** un `IdentidadActualPort` que lea
`SecurityContextHolder` de forma implícita. Añade estado ambiental, dificulta
las pruebas y oculta la dependencia. Es preferible que el adaptador web reciba
el principal de forma explícita.

### 5.3 Autorización por membresía

**Decisión propuesta:** autorización de objeto en el caso de uso, mediante un
puerto de consulta del módulo `acceso`.

Contrato:

```text
AccesoQueryPort.buscarPermiso(usuarioId, portafolioId)
    -> Optional<PermisoPortafolio>
```

`PermisoPortafolio` expone capacidades, no el rol crudo:

```text
PermisoPortafolio(UUID portafolioId, boolean lectura)
```

En esta fase solo se necesita `lectura`. Si más adelante hay escritura o
administración, se añaden campos compatibles; no se expone el rol para que
`cuentas` no dependa del modelo interno de `acceso`.

La implementación:

1. Busca la membresía por `(usuario_id, portafolio_id)`.
2. Comprueba que el portafolio exista y esté activo.
3. Traduce `membresia.rol` a capacidades con una política explícita.
4. Devuelve `Optional.empty()` si no hay membresía, el portafolio no existe o
   está inactivo.

El caso de uso de cuentas:

```text
execute(query):
    permiso = accesoQueryPort.buscarPermiso(query.usuarioId, query.portafolioId)
    si permiso está vacío -> NotFoundException (404)
    si permiso.lectura es falso -> AccesoDenegadoException (403, futuro)
    listar cuentas del portafolio
```

**Semántica elegida:** `404` cuando no hay membresía, el portafolio no existe o
está inactivo. No se devuelve `403` en ese caso porque confirmaría la existencia
del portafolio a un usuario sin acceso. `403` se reserva para un usuario
autenticado con membresía cuyo rol no permite la capacidad solicitada; hoy
todos los roles permiten lectura, así que ese caso no es alcanzable.

**Matriz de roles propuesta** (pendiente de aprobación de producto; no está
implementada y no cambia el esquema):

| Rol | Leer | Escribir | Administrar membresías |
|---|---|---|---|
| `LECTOR` | Sí | No | No |
| `EDITOR` | Sí | Sí | No |
| `ADMINISTRADOR` | Sí | Sí | Sí |
| `PROPIETARIO` | Sí | Sí | Sí |

Las operaciones destructivas del portafolio (borrado, transferencia de
propiedad) quedan fuera de esta matriz hasta que exista una decisión explícita.

**Por qué no `@PreAuthorize` para el rol del portafolio:** el rol depende del
`portafolioId` de la ruta y de la membresía, no de una autoridad global. Una
expresión `@PreAuthorize` necesitaría consultar la base y duplicaría la lógica
del caso de uso. Spring Security autentica; la aplicación autoriza el objeto.
`@PreAuthorize` podrá usarse para endpoints administrativos globales, no para
sustituir esta comprobación.

### 5.4 `execute()`

**Decisión propuesta:** el método canónico de los puertos de entrada será
`execute(...)`, tanto para consultas como para comandos.

```java
public interface ListarCuentasPorPortafolioUseCase {
    List<CuentaFinancieraView> execute(ListarCuentasPorPortafolioQuery query);
}
```

Reglas:

- Cada puerto sigue siendo **específico** de un caso de uso. No se crea
  `UseCase<I, O>` ni un bus de comandos.
- La transacción sigue en la implementación:
  `@Transactional(readOnly = true)` en `execute`.
- El controlador llama a `execute`; no cambia el contrato HTTP.
- El nombre `execute` sustituye a `consultar` y unifica el `ejecutar`/
  `consultar` que proponía la documentación de arquitectura.
- No se mantiene un alias `consultar` en paralelo: sería una segunda forma de
  invocar el mismo caso de uso y complicaría las pruebas.
- El cambio es mecánico y no altera comportamiento.

**Alternativas rechazadas:**

- `UseCase<I, O>` genérico: pierde tipos específicos, oculta el caso de uso y
  choca con la regla R12 de `module-boundaries.md`.
- `CommandBus`/`UseCaseExecutor` con un mapa de handlers: dispatch implícito,
  reflexión y dificultad para ArchUnit.
- Strategy por tipo de comando: no hay variación real; solo añade indirección.

### 5.5 Strategy

El patrón Strategy se aplica **solo donde hay una variación real**. Inventarlo
en cada capa sería una abstracción innecesaria.

| Punto de variación | ¿Strategy ahora? | Decisión |
|---|---|---|
| Mecanismo de autenticación (Basic, OIDC, JWT) | Sí, mediante el `AuthenticationProvider` de Spring Security | No se crea una interfaz propia; se añaden proveedores cuando haga falta |
| Política de roles del portafolio | No como clase por rol | Tabla explícita `RolMembresia -> capacidades` en `PoliticaCapacidadesPortafolio` |
| Enmascaramiento del número de cuenta | No | Un solo algoritmo; se extraerá una estrategia solo si se añade revelación total auditada |
| Ejecución de casos de uso | No | Un puerto por caso de uso con `execute`; prohibido el bus genérico |
| Persistencia | No | JPA explícito y mappers manuales; no hay Strategy de repositorios |

La política de roles se implementará como una clase pura y concreta:

```text
PoliticaCapacidadesPortafolio
    capacidadesDe(RolMembresia) -> Set<CapacidadPortafolio>
```

Si en el futuro aparece una segunda política real (por tipo de portafolio, por
tenant o por normativa), se extrae una interfaz `EstrategiaCapacidades` y se
selecciona la implementación. **No se crea la interfaz ahora**: una sola
implementación idéntica para todos los roles no justifica el patrón.

### 5.6 Semántica HTTP

| Situación | HTTP | Código | Quién responde |
|---|---|---|---|
| Sin credenciales o credenciales inválidas | `401` | `NO_AUTENTICADO` | `AuthenticationEntryPoint` |
| Usuario inactivo | `401` | `NO_AUTENTICADO` | `AuthenticationEntryPoint` |
| Autenticado sin membresía | `404` | `NO_ENCONTRADO` | Caso de uso + `ProblemDetail` |
| Portafolio inexistente o inactivo | `404` | `NO_ENCONTRADO` | Caso de uso + `ProblemDetail` |
| Membresía sin capacidad | `403` | `ACCESO_DENEGADO` | Caso de uso -> `AccesoDenegadoException` -> `@RestControllerAdvice` |
| UUID inválido | `400` | `ID_INVALIDO` | `CuentaFinancieraExceptionHandler` |
| Error interno | `500` | `ERROR_INTERNO` | `CuentaFinancieraExceptionHandler` |

El `401` debe incluir `WWW-Authenticate` y un `ProblemDetail`
`application/problem+json` coherente con el resto de la API. No se incluyen
detalles de por qué falló la autenticación.

### 5.7 Interacción con la exposición local/producción

La protección actual se conserva:

- Sin perfil explícito, el controlador no se registra.
- `local`/`test` + `expuesta=true` lo registra.
- `prod` lo mantiene deshabilitado.
- `prod` + `expuesta=true` falla al arrancar.

La autenticación **no sustituye** esta puerta en el primer incremento. Se
mantiene como defensa en profundidad. La decisión de exponer el endpoint en
producción es una fase posterior y requiere:

1. Autenticación y autorización probadas.
2. TLS terminado en un proxy o en el servidor.
3. Revisión de la guarda de producción.
4. Auditoría y limitación de intentos.
5. Aprobación explícita.

**Efecto colateral a documentar:** con una cadena de seguridad global para
`/api/**`, una petición anónima a un endpoint no registrado puede recibir `401`
antes de que Spring MVC resuelva el `404`. La garantía de “el controlador no se
registra” se seguirá verificando a nivel de bean y con un usuario autenticado.
Es un cambio deliberado de contrato, no un fallo de la fase 3.1.

---

## 6. Diseño propuesto

### 6.1 Paquetes y componentes

Se reutiliza la estructura existente. Los elementos nuevos se colocan así:

```text
com.patrimonio.acceso
├── api
│   ├── dto
│   │   ├── PortafolioResumen.java          (existente)
│   │   ├── PermisoPortafolio.java          (nuevo)
│   │   └── UsuarioAutenticado.java         (nuevo, contrato del principal)
│   └── port
│       ├── PortafolioQueryPort.java        (existente)
│       └── AccesoQueryPort.java            (nuevo)
├── domain
│   ├── model
│   │   ├── RolMembresia.java               (nuevo)
│   │   └── CapacidadPortafolio.java        (nuevo)
│   └── service
│       └── PoliticaCapacidadesPortafolio.java (nuevo, Java puro)
├── application
│   └── port.out
│       ├── UsuarioCredencialesRepository.java (nuevo, específico)
│       └── ...
└── adapter
    ├── in.security
    │   ├── SecurityConfig.java             (nuevo)
    │   ├── UsuarioDetailsService.java      (nuevo)
    │   ├── UsuarioDetails.java             (nuevo)
    │   ├── RestAuthenticationEntryPoint.java (nuevo)
    │   └── RestAccessDeniedHandler.java    (nuevo)
    └── out.persistence
        ├── entity
        │   ├── PortafolioEntity.java       (existente)
        │   ├── UsuarioEntity.java          (nuevo)
        │   └── MembresiaEntity.java        (nuevo)
        ├── repository
        │   ├── PortafolioJpaRepository.java (existente)
        │   ├── UsuarioJpaRepository.java    (nuevo)
        │   └── MembresiaJpaRepository.java  (nuevo)
        └── adapter
            ├── PortafolioQueryAdapter.java  (existente)
            ├── AccesoQueryAdapter.java      (nuevo)
            └── UsuarioCredencialesAdapter.java (nuevo)
```

No se crea un módulo nuevo: `acceso` ya es el propietario de `usuario`,
`portafolio` y `membresia` según `module-boundaries.md`.

### 6.2 Flujo de una petición

```text
Cliente
  -> HTTPS + Authorization: Basic ...
  -> SecurityFilterChain (/api/**)
       -> UsuarioDetailsService -> usuario (email, hash, activo)
       -> PasswordEncoder verifica
       -> SecurityContext con UsuarioDetails
  -> CuentaFinancieraController
       -> @AuthenticationPrincipal UsuarioAutenticado
       -> ListarCuentasPorPortafolioQuery(usuario.id(), portafolioId)
  -> ListarCuentasPorPortafolioService.execute(...)
       -> AccesoQueryPort.buscarPermiso(usuarioId, portafolioId)
            -> membresia + portafolio activo
            -> PoliticaCapacidadesPortafolio
       -> si no hay permiso: NotFoundException (404)
       -> CuentaFinancieraRepository.listarPorPortafolio(...)
  -> CuentaFinancieraResponseAssembler (máscara)
  -> 200 JSON
```

Puntos clave:

- El `usuarioId` **nunca** viene del cliente; viene del principal autenticado.
- La comprobación de membresía ocurre antes de consultar las cuentas.
- Si no hay permiso, no se llama al repositorio de cuentas.
- La transacción sigue siendo de solo lectura y se declara en el servicio.
- El adaptador web no conoce entidades JPA ni repositorios.

### 6.3 Contratos públicos

```java
// acceso.api.port
public interface AccesoQueryPort {
    Optional<PermisoPortafolio> buscarPermiso(UUID usuarioId, UUID portafolioId);
}

// acceso.api.dto
public record PermisoPortafolio(UUID portafolioId, boolean lectura) {
}

// acceso.api.dto
public interface UsuarioAutenticado {
    UUID id();
    String email();
    String nombre();
}
```

`UsuarioAutenticado` es una interfaz y no un `record` porque el principal de
Spring Security (`UsuarioDetails`) la implementa directamente. No contiene
contraseña, hash ni roles globales.

### 6.4 Persistencia reutilizada

No se modifica el esquema. Se reutilizan:

| Tabla | Uso | Restricción existente |
|---|---|---|
| `usuario` | Credenciales y estado | `uq_usuario_email`, `ck_usuario_email`, `idx_usuario_activo` |
| `portafolio` | Existencia y estado | `idx_portafolio_moneda_base`; `activo` |
| `membresia` | Autorización por portafolio | `uq_membresia_usuario_portafolio`, `ck_membresia_rol`, `idx_membresia_portafolio` |

`MembresiaEntity` usará `@Enumerated(EnumType.STRING)` para `rol`. Un valor
desconocido en la base debe fallar de forma explícita; no se convierte
silenciosamente en `null`.

Consulta propuesta (JPQL, no ejecutada en esta fase):

```text
select m
from MembresiaEntity m
where m.usuarioId = :usuarioId
  and m.portafolioId = :portafolioId
  and exists (
      select p.id
      from PortafolioEntity p
      where p.id = :portafolioId
        and p.activo = true
  )
```

No se crean asociaciones JPA entre `MembresiaEntity`, `UsuarioEntity` y
`PortafolioEntity`; se usan UUID, conforme a la regla R3 de
`module-boundaries.md`. No se necesita un índice nuevo.

### 6.5 Configuración de Spring Security

Dependencias que se añadirán **solo con autorización**:

```kotlin
implementation("org.springframework.boot:spring-boot-starter-security")
testImplementation("org.springframework.security:spring-security-test")
```

Las versiones las administra el BOM de Spring Boot 4.1.2. No se añade ninguna
librería de JWT, OAuth2 ni criptografía adicional.

Configuración propuesta:

- `SecurityFilterChain` con `securityMatcher("/api/**")`.
- `authorizeHttpRequests`: cualquier petición a `/api/**` requiere
  autenticación.
- `httpBasic`.
- `sessionManagement(STATELESS)`.
- `csrf.disable()` mientras no haya cookies.
- `requestCache.disable()`.
- `logout.disable()`.
- `headers`: valores por defecto; `frameOptions.deny`.
- `exceptionHandling` con entry point y denied handler que devuelven
  `ProblemDetail`.
- `PasswordEncoder` BCrypt.
- `UserDetailsService` propio.
- Sin `@EnableMethodSecurity` en el primer incremento; no se necesita para la
  autorización de objeto.

No se configura CORS comodín. Si aparece un frontend, se definirán orígenes
explícitos y se revisará CSRF.

### 6.6 Matriz de roles

La matriz de la sección 5.3 es la única fuente de verdad. Se implementa en
`PoliticaCapacidadesPortafolio` con un `Map<RolMembresia, Set<CapacidadPortafolio>>`
inmutable. No hay una clase por rol.

Consecuencias:

- `LECTOR`, `EDITOR`, `ADMINISTRADOR` y `PROPIETARIO` pueden leer.
- El endpoint actual solo necesita `LEER`.
- Las capacidades de escritura y administración se añadirán cuando existan sus
  casos de uso; no se implementan ahora.

### 6.7 Configuración y perfiles

- La seguridad se activa en todos los perfiles, incluido `test`.
- `local` sigue necesitando `expuesta=true` y perfil explícito para registrar
  el controlador.
- Para probar manualmente en local hará falta un usuario y una membresía en la
  base local. **No se automatiza un seeder ni se insertan datos desde la
  aplicación.** Mientras no exista el alta de usuarios, el desarrollador puede
  crear esas filas manualmente en su base local; no se versionan credenciales.
- No se usa un usuario en memoria ni una cabecera de desarrollo: sería una
  segunda vía de autenticación y un riesgo de que se active por error.
- `prod` mantiene `expuesta=false` y la guarda de arranque.

---

## 7. Reutilización, ampliación y novedad por brecha

| Brecha | Se reutiliza | Se amplía | Es realmente nuevo |
|---|---|---|---|
| Autenticación | `usuario`, `password_hash`, `activo`, `email`; patrón de adaptador de `acceso`; Spring Boot | `acceso` con entidades/repositorios de usuario; controlador con principal | `spring-boot-starter-security`, `SecurityConfig`, `UsuarioDetailsService`, `UsuarioDetails`, entry point, denied handler |
| Autorización | `membresia`, `rol`, UNIQUE e índices; `NotFoundException` y `ProblemDetail`; transacción del servicio | `AccesoQueryPort` y `PermisoPortafolio` (ya propuestos en la arquitectura); `ListarCuentasPorPortafolioQuery` con `usuarioId`; comprobación en el servicio | `RolMembresia`, `CapacidadPortafolio`, `PoliticaCapacidadesPortafolio`, `MembresiaEntity`, `MembresiaJpaRepository`, `AccesoQueryAdapter` |
| `execute()` | Puerto, servicio, controlador y pruebas existentes; `@Transactional` | Renombrar `consultar` a `execute` y actualizar llamadas | Nada: cambio mecánico |
| Strategy | `AuthenticationProvider`/`PasswordEncoder` de Spring Security; idea de política de roles | `PoliticaCapacidadesPortafolio` como punto de variación | Interfaz `EstrategiaCapacidades` solo si aparece una segunda política; no ahora |
| Exposición | `PerfilDeDesarrolloExplicito`, `@ConditionalOnProperty`, `CuentasApiExposicionGuard` | Documentar la interacción con seguridad | Decisión futura de exposición en producción; no se implementa ahora |

---

## 8. Plan por fases

Cada fase debe poder revisarse y probarse por separado. **Ninguna fase se
considera terminada si deja el endpoint accesible sin autenticación o sin
autorización.**

### Fase 1 - `execute()` sin cambio de comportamiento

Objetivo: unificar el nombre del método de entrada.

Archivos a modificar:

- `src/main/java/com/patrimonio/cuentas/application/port/in/ListarCuentasPorPortafolioUseCase.java`
- `src/main/java/com/patrimonio/cuentas/application/service/ListarCuentasPorPortafolioService.java`
- `src/main/java/com/patrimonio/cuentas/adapter/in/web/CuentaFinancieraController.java`
- `src/test/java/com/patrimonio/cuentas/application/service/ListarCuentasPorPortafolioServiceTest.java`
- `src/test/java/com/patrimonio/cuentas/adapter/in/web/CuentaFinancieraControllerTest.java`
- `src/test/java/com/patrimonio/cuentas/adapter/in/web/CuentaFinancieraApiLocalTest.java`

Criterios:

- `consultar` desaparece del puerto y del servicio.
- El contrato HTTP no cambia.
- Las 64 pruebas unitarias y las 13 de integración siguen pasando.
- ArchUnit sigue pasando.

Riesgo: bajo. Es un renombrado mecánico.

### Fase 2 - Modelo de lectura de `acceso`

Objetivo: exponer permisos de portafolio sin tocar todavía el endpoint.

Archivos nuevos:

- `acceso/domain/model/RolMembresia.java`
- `acceso/domain/model/CapacidadPortafolio.java`
- `acceso/domain/service/PoliticaCapacidadesPortafolio.java`
- `acceso/api/dto/PermisoPortafolio.java`
- `acceso/api/port/AccesoQueryPort.java`
- `acceso/adapter/out/persistence/entity/UsuarioEntity.java`
- `acceso/adapter/out/persistence/entity/MembresiaEntity.java`
- `acceso/adapter/out/persistence/repository/UsuarioJpaRepository.java`
- `acceso/adapter/out/persistence/repository/MembresiaJpaRepository.java`
- `acceso/adapter/out/persistence/adapter/AccesoQueryAdapter.java`
- `acceso/application/port/out/UsuarioCredencialesRepository.java`
- `acceso/adapter/out/persistence/adapter/UsuarioCredencialesAdapter.java`

Pruebas:

- `PoliticaCapacidadesPortafolioTest` (unitaria, sin Spring).
- `AccesoQueryAdapterIT` con Testcontainers: membresía activa, portafolio
  inactivo, sin membresía, cada rol.
- `UsuarioCredencialesAdapterIT`: búsqueda por email, usuario inactivo,
  normalización de email.

Criterios:

- No se modifica el endpoint ni su contrato.
- No se modifica el esquema.
- Las pruebas nuevas usan solo Testcontainers; nunca la base de desarrollo.
- ArchUnit: `cuentas` solo puede importar `acceso.api`; se mantiene.

### Fase 3 - Autenticación y autorización en el endpoint

Objetivo: el endpoint exige identidad y membresía. Esta fase se despliega como
un solo incremento; no se separa autenticación de autorización.

Cambios principales:

- Añadir dependencias de seguridad a `build.gradle.kts`.
- Crear `acceso/adapter/in/security/*`.
- Crear `acceso/api/dto/UsuarioAutenticado.java`.
- Añadir `usuarioId` a `ListarCuentasPorPortafolioQuery`.
- Inyectar `AccesoQueryPort` en `ListarCuentasPorPortafolioService` y comprobar
  el permiso antes de listar.
- Pasar el principal desde `CuentaFinancieraController`.
- Ajustar `CuentaFinancieraExceptionHandler` solo si hace falta para `401`/
  `403`; la autenticación la resuelve Spring Security antes del controlador.
- Actualizar pruebas y añadir pruebas de seguridad.

Pruebas mínimas:

| Escenario | Resultado esperado |
|---|---|
| Sin `Authorization` | `401`, `WWW-Authenticate`, sin llamar al caso de uso |
| Credenciales inválidas | `401` genérico |
| Usuario inactivo | `401` |
| Miembro `LECTOR` | `200` con cuentas enmascaradas |
| Miembro `EDITOR`/`ADMINISTRADOR`/`PROPIETARIO` | `200` |
| Autenticado sin membresía | `404`, sin consultar cuentas |
| Portafolio inexistente | `404`, misma respuesta que sin membresía |
| Portafolio inactivo | `404` |
| Sin perfil explícito | Controlador no registrado; autenticado -> `404`; anónimo -> `401` por seguridad |
| `prod` por defecto | Controlador no registrado; `expuesta=true` -> fallo de arranque |
| Máscara | Sigue mostrando `****` + últimos 4 |
| Logs | Sin contraseña, hash, token ni número completo |

Criterios:

- No se expone el endpoint en `prod`.
- No se acepta `usuarioId` desde la ruta, query ni body.
- No se llama al repositorio de cuentas si falta el permiso.
- Las pruebas de integración usan Testcontainers y datos de prueba propios.
- ArchUnit: dominio, aplicación y `api` no dependen de Spring Security; solo
  los adaptadores de seguridad y el adaptador web (por
  `@AuthenticationPrincipal`) pueden hacerlo.

### Fase 4 - Endurecimiento y decisión de producción

Objetivo: cerrar los pendientes antes de usuarios reales.

- Revisar `CuentasApiExposicionGuard` y decidir si se sustituye por una guarda
  que exija seguridad activa y TLS.
- Añadir limitación de intentos de autenticación y política de contraseñas.
- Evaluar auditoría persistida (requiere propuesta de esquema; no se hace
  ahora).
- Evaluar CORS si aparece un frontend.
- Añadir alta de usuarios, alta de membresías y revocación.
- Definir la semántica de `403` para roles sin capacidad.
- Actualizar la documentación de arquitectura, `cuentas-financieras.md`,
  `backend-bootstrap.md` y `README.md`.

Esta fase requiere autorización separada. **No se incluye en el primer
incremento.**

---

## 9. Pruebas

### 9.1 Unitarias (sin Spring ni Docker)

| Clase | Qué verifica |
|---|---|
| `PoliticaCapacidadesPortafolioTest` | Matriz de roles, capacidades y ausencia de mutabilidad |
| `ListarCuentasPorPortafolioServiceTest` | Permiso concedido, sin membresía -> `404`, sin capacidad -> `403` (cuando exista), error del repositorio, consulta nula |
| `UsuarioDetailsTest` | `isEnabled`, autoridades globales y ausencia de hash en `toString` |

### 9.2 Web (`@WebMvcTest`)

- Contrato `200`/`400`/`404`/`422`/`500` con un principal autenticado.
- `401` sin credenciales.
- `404` con usuario autenticado sin membresía.
- Verificación de que el controlador no se registra sin perfil o en `prod`.
- Las pruebas de contrato que no quieran ejercitar seguridad pueden usar
  `@AutoConfigureMockMvc(addFilters = false)`; las de seguridad deben usar la
  cadena real.

**Nota de implementación:** `@WithMockUser` crea un `User` de Spring, no un
`UsuarioDetails`. Para `@AuthenticationPrincipal UsuarioAutenticado` se usará
`@WithUserDetails` con un `UserDetailsService` de prueba o una anotación
`@WithMockUsuario` que construya el principal correcto.

### 9.3 Integración (Testcontainers)

- Contexto completo con `@SpringBootTest`, `@AutoConfigureMockMvc`,
  `@ActiveProfiles("test")`.
- PostgreSQL 16 efímero con `withInitScript("esquema.sql")`.
- Datos de prueba insertados con `JdbcTemplate` dentro del contenedor:
  usuario, portafolio, membresía y cuentas.
- Hash generado con el `PasswordEncoder` de la aplicación; no se hardcodean
  credenciales reales.
- Peticiones con `httpBasic(email, password)`.
- Limpieza en orden inverso a las claves foráneas, solo dentro del contenedor.
- Prohibido conectarse a `patrimonio-postgres` o a la base de desarrollo.

Casos:

1. Miembro `LECTOR` -> `200` y contrato completo.
2. Usuario autenticado sin membresía -> `404`.
3. Portafolio inexistente -> `404`.
4. Portafolio inactivo -> `404`.
5. Credenciales inválidas -> `401`.
6. Usuario inactivo -> `401`.
7. Sin perfil -> controlador ausente; autenticado -> `404`; anónimo -> `401`.
8. `prod` + `expuesta=true` -> fallo de arranque.
9. La máscara y el contrato de errores no cambian.

### 9.4 ArchUnit

Se mantienen las 12 reglas actuales y se añaden, como mínimo:

- `..domain..` no depende de `org.springframework.security..`.
- `..application..` no depende de `org.springframework.security..`.
- `..api..` no depende de `org.springframework.security..`.
- `SecurityContextHolder` solo se usa en `acceso.adapter.in.security..`.
- `acceso` sigue sin depender de `cuentas`.
- `cuentas` sigue usando solo `acceso.api`.
- No hay ciclos entre módulos.

El adaptador web de `cuentas` podrá usar
`org.springframework.security.core.annotation.AuthenticationPrincipal`; no
podrá usar `SecurityContextHolder` ni configuración de seguridad.

### 9.5 Comandos y criterios

Cuando se autorice la implementación, la validación será:

```powershell
$env:JAVA_HOME="C:\Users\jerry\AppData\Local\Temp\opencode\toolchain\jdk\jdk-25.0.5+8"
$env:Path="$env:JAVA_HOME\bin;$env:Path"
Set-Location D:\Develop\patrimonio
.\gradlew.bat compileJava test integrationTest check --rerun-tasks jacocoMergedReport --no-daemon --console=plain
```

- No se afirmará que una tarea pasó si no se ejecutó.
- Si falta Docker o Java 25, se documentará la limitación exacta.
- Las pruebas de integración usarán exclusivamente Testcontainers.
- No se ejecutará DDL ni DML contra la base de desarrollo.

---

## 10. Riesgos y decisiones pendientes

| # | Riesgo o decisión | Impacto | Propuesta |
|---|---|---|---|
| 1 | HTTP Basic sobre HTTP sin TLS | Credenciales expuestas | Exigir TLS; no exponer fuera de local sin proxy TLS |
| 2 | Seguridad global cambia `404` por `401` en endpoints ausentes | Pruebas y contrato de la fase 3.1 | Documentar y ajustar pruebas; verificar registro de bean |
| 3 | No hay alta de usuarios ni membresías | No se puede probar manualmente en local sin insertar filas | Implementar casos de uso de alta o documentar el paso manual local; no crear un seeder automático |
| 4 | Matriz de roles sin aprobar | Autorización incorrecta | Aprobar la matriz antes de implementar escrituras |
| 5 | `403` vs `404` sin membresía | Enumeración de portafolios | Mantener `404`; revisar con producto |
| 6 | Sin auditoría persistida | Difícil investigar accesos | Logs estructurados ahora; tabla de auditoría requiere propuesta de esquema |
| 7 | Sin limitación de intentos | Fuerza bruta | Añadir en fase 4 |
| 8 | Sin MFA, recuperación ni política de contraseñas | Gestión de identidad incompleta | Fuera de alcance; decidir antes de producción |
| 9 | `@AuthenticationPrincipal` acopla el adaptador web a Spring Security | Pureza del adaptador | Aceptable: es un adaptador de entrada; ArchUnit limita el resto |
| 10 | `execute` en inglés en un código con nombres en español | Consistencia | Usar `execute` como pidió el requisito; documentar la decisión |
| 11 | Dependencia nueva de Spring Security | Aprobación y superficie | Añadir solo con autorización; el BOM gestiona la versión |
| 12 | `PortafolioQueryPort` deja de usarse en el flujo de cuentas | Posible confusión | Se mantiene como API para otros módulos; el flujo de cuentas usa `AccesoQueryPort` |
| 13 | Rol desconocido en la base | Fallo de mapeo | Falla explícita y log; no se convierte a `null` |
| 14 | Email con mayúsculas | Duplicados o login fallido | Normalizar en alta y búsqueda; el UNIQUE actual es sensible a mayúsculas |

---

## 11. Criterios de aceptación del plan

1. El documento distingue lo completo, parcial, pendiente y no verificado.
2. Cada brecha indica qué se reutiliza, qué se amplía y qué es nuevo.
3. La autenticación y la autorización se integran en `acceso` y `cuentas` sin
   crear un módulo ni un controlador paralelo.
4. `execute(...)` sustituye a `consultar(...)` sin cambiar el contrato HTTP.
5. Strategy se aplica solo donde hay variación real; no se crea un bus genérico.
6. La autorización por membresía se comprueba antes de consultar las cuentas.
7. El endpoint sigue sin exponerse en producción.
8. No se modifica el esquema, la base de datos, los contenedores ni Git.
9. No se declara ninguna prueba como pasada sin haberla ejecutado.
10. La implementación queda bloqueada hasta autorización explícita.

---

## 12. Lo que NO se hace en esta fase

- No se modifica código de aplicación.
- No se modifica `db/esquema.sql`.
- No se ejecuta DDL ni DML.
- No se modifican `docker-compose.yml`, `.env`, `.env.example` ni el contenedor
  `patrimonio-postgres`.
- No se añaden dependencias.
- No se crean usuarios, membresías ni datos ficticios.
- No se implementa login, filtros, `SecurityConfig` ni `execute`.
- No se hacen commits ni pushes.
- No se actualizan los documentos de arquitectura existentes; las discrepancias
  quedan registradas aquí y se corregirán cuando se autorice la implementación.

---

## 13. Referencias

- `docs/architecture/architecture-overview.md` - visión general y seguridad
  pendiente.
- `docs/architecture/hexagonal-architecture.md` - capas, puertos y convención
  `ejecutar`/`consultar`.
- `docs/architecture/module-boundaries.md` - propiedad de `usuario`,
  `portafolio` y `membresia`; `AccesoQueryPort`; reglas R1-R16.
- `docs/architecture/testing-strategy.md` - niveles de prueba y ausencia actual
  de pruebas de seguridad.
- `docs/cuentas-financieras.md` - contrato del endpoint, seguridad pendiente y
  pruebas existentes.
- `docs/backend-bootstrap.md` - perfiles, exposición y comandos.
- `db/esquema.sql` - tablas `usuario`, `portafolio` y `membresia`; no se
  modifica.
