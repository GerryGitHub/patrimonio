# Patrimonio — Autenticación y autorización (MVP)

> Documento de la fase de seguridad del MVP. Fecha: 2026-10-30.
>
> Implementa el registro de usuarios, el inicio y cierre de sesión, la
> revocación de credenciales, la autorización por membresía y la protección de
> los endpoints de portafolios y cuentas. No modifica el esquema PostgreSQL, no
> incluye recuperación de contraseña ni verificación de correo y no introduce
> migraciones automáticas.

## 1. Estrategia elegida

Se usa **sesión de servidor con cookie `HttpOnly`** para clientes web y móviles.
No se emiten tokens de acceso ni tokens de refresco.

| Decisión | Valor |
|----------|-------|
| Mecanismo | Sesión de servidor (`HttpSession`) |
| Cookie de sesión | `PATRIMONIO_SESSION` |
| Cookie CSRF | `XSRF-TOKEN` (legible por el cliente) |
| Cabecera CSRF | `X-XSRF-TOKEN` |
| Inactividad máxima | 30 minutos (`server.servlet.session.timeout`) |
| Duración absoluta | 8 horas (`patrimonio.seguridad.sesion.duracion-maxima`) |
| Almacenamiento de la credencial | Cookie `HttpOnly`; nunca `localStorage` |
| Hash de contraseña | BCrypt adaptativo con sal, coste 10 |
| Renovación | No hay endpoint de refresco |

### 1.1 Por qué sesión de servidor

- El cliente nunca recibe un token de acceso reutilizable en el cuerpo de la
  respuesta: solo recibe un identificador de sesión opaco en una cookie
  `HttpOnly`.
- El servidor puede invalidar la credencial de inmediato con `logout`, sin
  esperar a que caduque un JWT.
- El mismo mecanismo sirve para web y para móvil: ambos clientes mantienen una
  cookie jar y envían la cookie en cada petición.
- No hay que implementar rotación, revocación ni detección de reutilización de
  refresh tokens, porque no existen.

El coste es que la sesión vive en memoria del proceso: un reinicio o un
despliegue invalidan las sesiones, y el escalado horizontal exige sesiones
pegajosas o un almacén compartido (ver limitaciones).

### 1.2 Por qué no hay `POST /api/auth/refresh`

La sesión de servidor se renueva de forma implícita mientras el usuario esté
activo, hasta el límite absoluto de 8 horas. No hay un token de refresco que
rotar ni un identificador de refresco que pueda reutilizarse. Por tanto, el
endpoint de refresco **no aplica** en esta estrategia y no se publica.

## 2. Endpoints

### 2.1 `GET /api/auth/csrf`

Obtiene el token CSRF que el cliente debe enviar en las peticiones que modifican
estado. Es público y no crea sesión autenticada.

```http
GET /api/auth/csrf
```

Respuesta `200 OK`:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "0f5b0f4e-..."
}
```

La respuesta también establece la cookie `XSRF-TOKEN`. La cookie es legible por
JavaScript (`HttpOnly=false`) a propósito: el patrón *double-submit* necesita
que el cliente copie su valor a la cabecera. El token CSRF no autentica por sí
solo.

### 2.2 `POST /api/auth/registro`

Crea una cuenta de usuario. Es público, pero exige el token CSRF porque
modifica estado.

```http
POST /api/auth/registro
Content-Type: application/json
X-XSRF-TOKEN: <token de la cookie XSRF-TOKEN>

{
  "nombre": "María Pérez",
  "email": "maria@example.com",
  "password": "Secreta123!"
}
```

Respuesta `201 Created`:

```json
{
  "id": "99999999-9999-4999-8999-999999999999",
  "email": "maria@example.com",
  "nombre": "María Pérez"
}
```

El nombre y el correo se recortan; el correo se convierte a minúsculas antes de
comprobar duplicados y persistir. La contraseña debe tener entre 8 y 72 bytes
UTF-8 (límite de BCrypt) y se guarda únicamente como hash.

**El registro no crea sesión ni emite la cookie `PATRIMONIO_SESSION`.** El
cliente debe llamar después a `POST /api/auth/login`. Así el alta y el acceso
quedan desacoplados y el comportamiento es explícito para web y móvil.

Errores:

| Situación | HTTP | `code` |
|-----------|------|--------|
| Correo ya registrado (incluida la carrera entre comprobación e inserción) | `409` | `EMAIL_DUPLICADO` |
| Nombre, correo o contraseña inválidos; JSON mal formado | `400` | `VALIDACION_ENTRADA` |
| Falta el token CSRF o no coincide | `403` | `ACCESO_DENEGADO` |

La respuesta de duplicado no repite el correo enviado. El registro tampoco
devuelve contraseña, hash ni identificadores de sesión.

### 2.3 `POST /api/auth/login`

Verifica las credenciales y crea la sesión.

```http
POST /api/auth/login
Content-Type: application/json
X-XSRF-TOKEN: <token de la cookie XSRF-TOKEN>

{
  "usuario": "titular@example.com",
  "password": "Demo1234!"
}
```

Respuesta `200 OK`:

```json
{
  "mecanismo": "SESSION",
  "usuario": {
    "id": "11111111-1111-4111-8111-111111111111",
    "email": "titular@example.com",
    "nombre": "Titular de prueba"
  },
  "expiraEnSegundos": 1800,
  "duracionMaximaSegundos": 28800,
  "csrfToken": "nuevo-token-csrf"
}
```

La respuesta establece la cookie `PATRIMONIO_SESSION` y rota el token CSRF. El
cuerpo no contiene contraseña, hash, roles internos ni identificadores de
sesión.

Errores:

| Situación | HTTP | `code` |
|-----------|------|--------|
| Credenciales incorrectas, usuario inexistente o usuario inactivo | `401` | `NO_AUTENTICADO` |
| Cuerpo ausente, JSON mal formado o validación incumplida | `400` | `VALIDACION_ENTRADA` |
| Falta el token CSRF o no coincide | `403` | `ACCESO_DENEGADO` |

Los tres casos de credenciales fallidas devuelven exactamente la misma
respuesta. No se distingue entre usuario inexistente, contraseña incorrecta y
cuenta inactiva.

### 2.4 `GET /api/auth/me`

Devuelve la identidad de la sesión actual.

```http
GET /api/auth/me
```

Respuesta `200 OK`:

```json
{
  "id": "11111111-1111-4111-8111-111111111111",
  "email": "titular@example.com",
  "nombre": "Titular de prueba"
}
```

Sin sesión válida responde `401 NO_AUTENTICADO`.

### 2.5 `POST /api/auth/logout`

Invalida la sesión actual y elimina la cookie CSRF. Es idempotente: si no hay
sesión, también responde `204 No Content`.

```http
POST /api/auth/logout
X-XSRF-TOKEN: <token de la cookie XSRF-TOKEN>
```

Después del cierre, una petición con la cookie de sesión antigua recibe `401`.
El endpoint es público para que el cliente pueda cerrar sesión aunque su sesión
ya haya caducado; la operación solo afecta a la sesión presentada.

### 2.6 `GET /api/v1/portafolios`

Lista los portafolios activos sobre los que el usuario autenticado tiene
membresía. La identidad se toma del contexto de seguridad, nunca de un
parámetro.

```http
GET /api/v1/portafolios
```

Respuesta `200 OK`:

```json
[
  {
    "id": "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1",
    "nombre": "Portafolio A",
    "monedaBaseId": "a0000000-0000-4000-8000-000000000001"
  }
]
```

La consulta exige usuario activo, portafolio activo y membresía. El orden es
`nombre ASC, id ASC`. Sin membresías devuelve `[]`.

### 2.7 `GET /api/v1/portafolios/{portafolioId}/cuentas`

Lista las cuentas del portafolio si el usuario tiene membresía y permiso de
lectura.

```http
GET /api/v1/portafolios/aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1/cuentas
```

Respuesta `200 OK`:

```json
[
  {
    "id": "f0000000-0000-4000-8000-000000000001",
    "nombre": "Cuenta corriente",
    "numeroCuenta": "****1332",
    "institucionId": "b0000000-0000-4000-8000-000000000001",
    "tipoCuentaId": "c0000000-0000-4000-8000-000000000001",
    "monedaId": "a0000000-0000-4000-8000-000000000001",
    "activo": true,
    "fechaApertura": "2024-01-15",
    "fechaCierre": null
  }
]
```

El número de cuenta **nunca** viaja completo: se enmascara con `****` y los
últimos cuatro caracteres. El endpoint solo se registra con perfil `local` o
`test` explícito y `patrimonio.api.cuentas.expuesta=true`; en `prod` no existe
(404).

## 3. Ciclo de vida de la sesión

1. **Creación.** `POST /api/auth/login` verifica las credenciales en el caso de
   uso, crea la `HttpSession`, cambia el identificador con
   `changeSessionId()` y guarda el `SecurityContext` de forma explícita.
2. **Inactividad.** El contenedor invalida la sesión tras 30 minutos sin
   peticiones (`server.servlet.session.timeout`).
3. **Límite absoluto.** `AuthController` guarda el instante de inicio en la
   sesión y `SesionExpiracionFilter` la invalida cuando se superan 8 horas,
   aunque haya actividad. El filtro se ejecuta antes de cargar el contexto de
   seguridad, de modo que una sesión caducada no autentica la petición.
4. **Cierre.** `POST /api/auth/logout` invalida la sesión, limpia el contexto y
   borra la cookie CSRF.
5. **Revocación.** No hay un registro de sesiones por usuario ni un endpoint
   para revocar todas las sesiones. La revocación efectiva es el cierre de la
   sesión presentada y la caducidad. La desactivación de un usuario impide
   nuevos inicios de sesión y bloquea las consultas de portafolios y cuentas,
   porque esas consultas exigen usuario activo.

### 3.1 Fijación de sesión

Tras autenticar, se llama a `changeSessionId()` antes de guardar el contexto.
Un identificador de sesión conocido antes del login deja de ser válido. El
token CSRF también se regenera y se guarda de nuevo.

## 4. CSRF

La aplicación usa el patrón *double-submit cookie* de Spring Security:

- `GET /api/auth/csrf` entrega el token y establece la cookie `XSRF-TOKEN`.
- El cliente envía el valor en la cabecera `X-XSRF-TOKEN`.
- `CookieCsrfTokenRepository` compara la cookie con la cabecera.
- Se usa `CsrfTokenRequestAttributeHandler` con
  `setCsrfRequestAttributeName(null)`: el cliente envía el token tal cual, sin
  el enmascarado XOR pensado para formularios HTML.
- El token se rota en el login y se elimina en el logout.
- La cookie CSRF usa `SameSite=Lax`, `Path=/` y `Secure` cuando la cookie de
  sesión es segura.

Las peticiones `GET` no requieren token. `POST /api/auth/login`,
`POST /api/auth/logout` y cualquier futura operación de escritura sí lo
requieren. Sin token o con un token que no coincide, la respuesta es
`403 ACCESO_DENEGADO` y la operación no se ejecuta.

## 5. Almacenamiento en el cliente

### Web

- La cookie de sesión la gestiona el navegador; JavaScript no puede leerla
  (`HttpOnly`).
- El token CSRF se lee de la cookie `XSRF-TOKEN` o del cuerpo de
  `GET /api/auth/csrf` y se envía en la cabecera `X-XSRF-TOKEN`.
- No se guardan tokens en `localStorage` ni `sessionStorage`.
- Las peticiones deben usar `credentials: "include"` (fetch) o
  `withCredentials = true` (XHR) si el frontend está en otro origen.

### Móvil

- El cliente debe conservar la cookie jar de la plataforma (por ejemplo,
  `CookieManager` en Android o `HTTPCookieStorage` en iOS) y reenviar las
  cookies en cada petición.
- No debe extraer ni persistir el identificador de sesión en almacenamiento
  propio.
- El token CSRF se obtiene con `GET /api/auth/csrf` y se envía en la cabecera.

### Producción

`application-prod.yml` fuerza `server.servlet.session.cookie.secure=true`. La
cookie solo viaja por HTTPS. En local sobre HTTP se deja en `false` mediante
`SESSION_COOKIE_SECURE=false`; no debe usarse así en producción.

## 6. Contraseñas

- Las contraseñas se guardan como hash BCrypt con sal incorporada y coste 10
  (valor por defecto de Spring Security).
- Nunca se guardan en claro, no se cifran de forma reversible y no se
  registran en logs.
- El login normaliza el correo con `strip()` y minúsculas antes de buscar.
- La comparación se hace con `PasswordEncoder.matches`; un hash ausente o vacío
  no autentica.
- El contrato limita `usuario` a 254 caracteres y `password` a 72, el máximo
  útil de BCrypt.
- El registro público crea usuarios con hash BCrypt. No hay recuperación ni
  cambio de contraseña, verificación de correo ni usuario administrador en esta
  fase.

## 7. Autorización por membresía

La autorización de negocio vive en la capa de aplicación, no en el
controlador:

```text
CuentaFinancieraController
  -> ListarCuentasPorPortafolioUseCase.execute(query con usuarioId de la sesión)
       -> AccesoQueryPort.buscarPermiso(usuarioId, portafolioId)
       -> si no hay permiso: NotFoundException (404)
       -> si hay permiso sin lectura: AccesoDenegadoException (403)
       -> CuentaFinancieraRepository.listarPorPortafolio(portafolioId)
```

Reglas:

- El `usuarioId` sale de `@AuthenticationPrincipal`, poblado desde la sesión.
  No se acepta un identificador de usuario en la ruta, el cuerpo ni la query.
- La consulta de permiso exige simultáneamente: membresía, usuario activo y
  portafolio activo.
- Si no hay membresía, el portafolio no existe o está inactivo, o el usuario
  está inactivo, la respuesta es `404 NO_ENCONTRADO`. Así no se revela la
  existencia del portafolio.
- Si la membresía existe pero no concede lectura, la respuesta es
  `403 ACCESO_DENEGADO`. Con los roles actuales (`LECTOR`, `EDITOR`,
  `ADMINISTRADOR`, `PROPIETARIO`) todos conceden lectura, por lo que esta rama
  es defensiva y no se alcanza en producción.
- `GET /api/v1/portafolios` aplica el mismo filtro de usuario activo,
  portafolio activo y membresía.
- Las cuentas se consultan solo después de autorizar y solo del portafolio
  autorizado.

### 7.1 Respuestas de seguridad

| Situación | HTTP | `code` |
|-----------|------|--------|
| Sin sesión, sesión caducada o sesión revocada | `401` | `NO_AUTENTICADO` |
| Sesión válida sin permiso de lectura | `403` | `ACCESO_DENEGADO` |
| Sin membresía, portafolio inexistente o inactivo, usuario inactivo | `404` | `NO_ENCONTRADO` |
| Token CSRF ausente o inválido | `403` | `ACCESO_DENEGADO` |

Todas las respuestas de error usan RFC 9457 `application/problem+json` con un
campo `code` estable. No incluyen trazas, SQL, entidades JPA, hashes, tokens,
correos ni números de cuenta.

## 8. Arquitectura y archivos

La fase mantiene la arquitectura hexagonal:

- **Dominio/aplicación:** sin dependencias de Spring Security, HTTP ni JPA.
  `AutenticarUsuarioService` y `ListarPortafoliosAutorizadosService` implementan
  puertos de entrada con `execute()`.
- **Puertos de salida:** `UsuarioCredencialesPort`, `PasswordHasher`,
  `AccesoQueryPort`, `PortafolioQueryPort`.
- **Adaptadores de persistencia:** leen `usuario`, `portafolio` y `membresia`
  mediante JPA; no exponen entidades fuera del adaptador.
- **Adaptadores de seguridad:** `SecurityConfig`, `SesionExpiracionFilter`,
  `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`,
  `BCryptPasswordHasher`.
- **Adaptadores web:** `AuthController`, `PortafolioController`,
  `CuentaFinancieraController` y sus manejadores de errores.

Archivos principales:

```text
src/main/java/com/patrimonio/acceso/
├── adapter/in/security/SecurityConfig.java
├── adapter/in/security/SesionExpiracionFilter.java
├── adapter/in/security/RestAuthenticationEntryPoint.java
├── adapter/in/security/RestAccessDeniedHandler.java
├── adapter/in/web/AuthController.java
├── adapter/in/web/AuthExceptionHandler.java
├── adapter/in/web/PortafolioController.java
├── adapter/out/persistence/adapter/AccesoQueryAdapter.java
├── adapter/out/persistence/adapter/UsuarioCredencialesAdapter.java
├── adapter/out/persistence/repository/MembresiaJpaRepository.java
├── adapter/out/security/BCryptPasswordHasher.java
├── api/port/AccesoQueryPort.java
├── application/service/AutenticarUsuarioService.java
└── application/service/ListarPortafoliosAutorizadosService.java
```

La autorización de cuentas se incorpora en
`src/main/java/com/patrimonio/cuentas/application/service/ListarCuentasPorPortafolioService.java`.

## 9. Pruebas

### 9.1 Unitarias

Cubren la lógica de aplicación y los adaptadores aislados:

- `AutenticarUsuarioServiceTest`: credenciales válidas, contraseña incorrecta,
  usuario inexistente, usuario inactivo, normalización del correo y no
  distinción de errores.
- `ListarPortafoliosAutorizadosServiceTest`: lista autorizada y lista vacía.
- `SesionExpiracionFilterTest`: sesión dentro y fuera del límite absoluto.
- `BCryptPasswordHasherTest`: hash válido, hash inválido y entradas nulas.
- Pruebas de arquitectura hexagonal y de los guards de exposición.

```powershell
.\gradlew.bat test
```

### 9.2 Integración

Usan PostgreSQL 16 efímero con Testcontainers y aplican `db/esquema.sql` al
contenedor de prueba. **Nunca** se conectan a la base local ni al contenedor
`patrimonio-postgres`.

- `AuthApiIT`: login válido, inválido, inexistente e inactivo; contraseña
  almacenada como hash; `/me`; logout; sesión revocada; caducidad absoluta;
  CSRF y su rotación; ausencia de datos sensibles.
- `PortafolioApiIT`: listado autorizado, lista vacía y aislamiento entre
  usuarios.
- `CuentaFinancieraApiIT`: acceso autorizado, sin membresía, portafolio
  inexistente o inactivo, usuario inactivo, permisos insuficientes, ausencia de
  fugas y regresión del contrato.

```powershell
.\gradlew.bat integrationTest
```

`CsrfTestSupport` obtiene un token real con `GET /api/auth/csrf` y lo adjunta
como cookie y cabecera. No se usa
`SecurityMockMvcRequestPostProcessors.csrf()` porque ese post-procesador
sustituye por reflexión el repositorio del `CsrfFilter` compartido y contamina
las peticiones posteriores del mismo contexto de prueba.

### 9.3 Datos de prueba manuales

`docs/testing/datos-prueba-auth.sql` inserta usuarios, portafolios, membresías y
cuentas ficticias. El script comprueba que `usuario` esté vacía antes de
insertar y aborta si ya hay datos. Está pensado para una base desechable, no
para la base de desarrollo.

Credenciales ficticias:

| Usuario | Contraseña | Acceso |
|---------|------------|--------|
| `titular@example.com` | `Demo1234!` | `PROPIETARIO` del portafolio A |
| `lector@example.com` | `Lector1234!` | `LECTOR` del portafolio A |
| `sin-acceso@example.com` | `Editor1234!` | Sin membresías |
| `inactivo@example.com` | `Demo1234!` | Usuario inactivo |

El portafolio B está activo pero el titular no tiene membresía; el portafolio C
está inactivo y el titular sí tiene membresía. Ambos deben responder `404`.

Ejemplo de preparación con un contenedor efímero (nunca el de desarrollo):

```powershell
docker run --rm -d --name patrimonio-auth-seed -p 55432:5432 `
  -e POSTGRES_DB=patrimonio_test `
  -e POSTGRES_USER=patrimonio_test `
  -e POSTGRES_PASSWORD=patrimonio_test postgres:16

# Esperar a que acepte conexiones y aplicar esquema y datos:
psql "postgresql://patrimonio_test:patrimonio_test@localhost:55432/patrimonio_test" `
  -f db/esquema.sql -f docs/testing/datos-prueba-auth.sql
```

Después, arrancar la aplicación apuntando a ese contenedor y con el perfil
`local` o `test`, por ejemplo con las variables `POSTGRES_*` y
`patrimonio.api.cuentas.expuesta=true`.

### 9.4 Colección Postman

`docs/testing/patrimonio-auth.postman_collection.json` contiene 26 peticiones
agrupadas en seis carpetas. La guía paso a paso está en
`docs/testing/postman-guia.md`.

- `00 · Sin sesión`: comprueba `401` en `/me`, portafolios y cuentas.
- `01 · Autenticación`: CSRF, login del titular y `/me`.
- `02 · Portafolios y cuentas`: listado, cuentas autorizadas, portafolio ajeno
  (`404`), portafolio inactivo (`404`), portafolio inexistente (`404`) y UUID
  inválido (`400`).
- `03 · Casos negativos de login`: contraseña incorrecta, usuario inexistente,
  usuario inactivo, cuerpo vacío (`400`), JSON mal formado (`400`) y login sin
  CSRF (`403`).
- `04 · Usuario sin membresías`: login, lista vacía de portafolios y `404` al
  pedir cuentas.
- `99 · Cierre de sesión`: CSRF, logout y `/me` posterior (`401`).

Orden recomendado: `00 → 01 → 02 → 03 → 04 → 99`.

Uso:

1. Importar la colección en Postman.
2. Ajustar `baseUrl` si la API no está en `http://localhost:8080`.
3. Ejecutar las carpetas en orden. Postman conserva las cookies y los scripts
   guardan el token CSRF rotado en la variable `csrfToken`.
4. Ejecutar primero `00` con las cookies limpias; el login de `01` invalida el
   token CSRF anterior.
5. Si se repite la colección, borrar las cookies de `{{baseUrl}}` o ejecutar
   antes `99 · Cierre de sesión`.

La colección asume que los datos de `datos-prueba-auth.sql` están cargados en
una base desechable y que el endpoint de cuentas está expuesto (perfil `local` o
`test`).

### 9.5 Resultado de la última ejecución

Verificación completa ejecutada el 2026-10-30 con
`.\gradlew.bat check --rerun-tasks --no-daemon`:

| Suite | Pruebas | Fallos | Errores | Omitidas |
|-------|---------|--------|---------|----------|
| Unitarias (`test`) | 85 | 0 | 0 | 0 |
| Integración (`integrationTest`) | 32 | 0 | 0 | 0 |

`BUILD SUCCESSFUL`. Las pruebas de integración usaron contenedores PostgreSQL 16
efímeros; no se conectaron a la base local.

## 10. Limitaciones y trabajo pendiente

- **Sin límite de intentos ni bloqueo de cuenta.** No hay protección contra
  fuerza bruta más allá del coste de BCrypt. Debe añadirse rate limiting en el
  proxy o en la aplicación.
- **Coste de BCrypt.** Se usa el coste 10 por defecto de Spring Security. Es
  aceptable para el MVP, pero debe revisarse periódicamente según el hardware y
  el tiempo de respuesta objetivo.
- **Tiempo de respuesta no uniforme.** El hash solo se calcula cuando el usuario
  existe y está activo; un atacante con mediciones muy precisas podría inferir
  la existencia o el estado de una cuenta. La respuesta HTTP sí es idéntica.
- **Sin MFA.** No hay segundo factor.
- **Sin recuperación, cambio de contraseña ni verificación de correo.** El
  registro público crea la cuenta directamente, sin confirmar que el correo
  pertenece a quien se registra. Debe añadirse verificación y recuperación antes
  de producción.
- **Sesiones en memoria.** Un reinicio invalida todas las sesiones. El escalado
  horizontal requiere sesiones pegajosas o Spring Session con un almacén
  compartido.
- **Sin revocación global.** No se pueden listar ni cerrar todas las sesiones de
  un usuario.
- **`/me` no revalida el estado activo en cada petición.** La desactivación se
  aplica al iniciar sesión y en las consultas de portafolios y cuentas, que
  exigen usuario activo. Una sesión ya abierta puede seguir leyendo `/me`
  hasta caducar.
- **Sin HTTPS obligatorio en la aplicación.** En producción debe terminar TLS
  un proxy inverso y la cookie debe llevar `Secure` (ya configurado en
  `application-prod.yml`). Tampoco se configuran HSTS ni redirección a HTTPS.
- **Sin CORS.** El cliente web debe servirse desde el mismo origen que la API.
  Un frontend en otro origen necesita configuración CORS explícita y, con
  `SameSite=Lax`, probablemente `SameSite=None; Secure`.
- **CSRF y XSS.** El token CSRF es legible por JavaScript por diseño. La cookie
  de sesión es `HttpOnly`, pero una XSS en el frontend podría emitir peticiones
  en nombre del usuario. No se configura CSP en esta fase.
- **Endpoint de cuentas solo en desarrollo.** En producción responde `404`
  hasta que exista una decisión explícita de exposición.
- **Rama `403` por permiso insuficiente.** Implementada y probada, pero no
  alcanzable con los roles actuales, que conceden lectura.
- **Sin auditoría de seguridad.** No se registran intentos fallidos ni eventos
  de sesión en un almacén consultable.

## 11. Confirmaciones de alcance

- No se ha modificado `db/esquema.sql`, `docker-compose.yml`, `.env` ni
  `.env.example`.
- No se han ejecutado DDL ni DML contra la base local ni contra el contenedor
  `patrimonio-postgres`.
- No se han creado migraciones automáticas ni cambios de esquema.
- No se han añadido credenciales reales ni secretos al código o a archivos
  versionados.
- Los cambios de esta fase permanecen sin commit ni push.
