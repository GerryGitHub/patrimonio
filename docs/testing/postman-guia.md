# Guía rápida de Postman — API de Patrimonio

> Colección: `docs/testing/patrimonio-auth.postman_collection.json`
>
> Datos de prueba: `docs/testing/datos-prueba-auth.sql`
>
> Flujo completo desde base vacía: `docs/testing/guia-desde-cero.md`
>
> Documentación de seguridad: `docs/seguridad-autenticacion.md`

## 1. Qué se puede probar

La colección cubre lo que existe hoy en el backend:

| Área | Endpoints | Estado |
|------|-----------|--------|
| Registro | `POST /api/auth/registro` | Público, pero exige CSRF; no crea sesión |
| Autenticación | `GET /api/auth/csrf`, `POST /api/auth/login`, `GET /api/auth/me`, `POST /api/auth/logout` | Disponible en todos los perfiles |
| Portafolios | `GET /api/v1/portafolios` | Disponible con sesión |
| Cuentas | `GET /api/v1/portafolios/{id}/cuentas` | Solo con perfil `local` o `test` y `patrimonio.api.cuentas.expuesta=true` |
| Seguridad | CSRF, cookies, 401, 403, 404, 400, 409 | Cubierto por la colección |

No existen todavía:

- `POST /api/auth/refresh`: la estrategia es sesión de servidor, no tokens de
  acceso/refresco.
- Recuperación o cambio de contraseña.
- MFA.
- Usuario administrador.

## 2. Requisitos

1. Backend arrancado en `{{baseUrl}}` (por defecto `http://localhost:8080`).
2. Perfil `local` o `test` activo de forma explícita.
3. `patrimonio.api.cuentas.expuesta=true` para que exista el endpoint de cuentas.
4. Datos de `docs/testing/datos-prueba-auth.sql` cargados en una base
   **desechable**, nunca en la base local ni en `patrimonio-postgres`.

Ejemplo de arranque apuntando a una base desechable en el puerto `55432` y
publicando la API en `18080`:

```powershell
.\gradlew.bat bootRun --args="--spring.profiles.active=local --server.port=18080 --spring.datasource.url=jdbc:postgresql://localhost:55432/patrimonio_test --spring.datasource.username=patrimonio_test --spring.datasource.password=patrimonio_test --patrimonio.api.cuentas.expuesta=true"
```

En ese caso, cambia `baseUrl` en Postman a `http://localhost:18080`. El perfil
`local` ya define `patrimonio.api.cuentas.expuesta=true`; la propiedad del
ejemplo se incluye para que el comando sea explícito.

## 3. Importar la colección

1. Abre Postman.
2. `Import` → selecciona
   `docs/testing/patrimonio-auth.postman_collection.json`.
3. Revisa la variable `baseUrl`. Si la API no está en `http://localhost:8080`,
   cámbiala en la pestaña `Variables` de la colección.
4. No hace falta configurar autenticación en Postman: la sesión viaja en la
   cookie `PATRIMONIO_SESSION` y Postman la guarda automáticamente.

## 4. Variables

| Variable | Valor por defecto | Uso |
|----------|-------------------|-----|
| `baseUrl` | `http://localhost:8080` | URL base de la API |
| `usuario` | `titular@example.com` | Titular con membresía en el portafolio A |
| `password` | `Demo1234!` | Contraseña ficticia del titular |
| `usuarioSinAcceso` | `sin-acceso@example.com` | Usuario activo sin membresías |
| `passwordSinAcceso` | `Editor1234!` | Contraseña ficticia del usuario sin acceso |
| `usuarioInactivo` | `inactivo@example.com` | Usuario inactivo |
| `usuarioInexistente` | `nadie@example.com` | Correo no registrado |
| `registroNombre` | `Usuario Postman` | Nombre ficticio del usuario que se registra |
| `registroEmail` | vacío | Correo único; lo genera `00b` en cada ejecución |
| `registroPassword` | `Registro123!` | Contraseña ficticia del usuario registrado |
| `registroEmailInvalido` | vacío | Correo único para el caso de contraseña corta |
| `registroEmailSinCsrf` | vacío | Correo único para el caso de registro sin CSRF |
| `csrfToken` | vacío | Lo rellenan los scripts |
| `portafolioId` | UUID del portafolio A | Portafolio autorizado |
| `portafolioAjenoId` | UUID del portafolio B | Activo, sin membresía |
| `portafolioInactivoId` | UUID del portafolio C | Inactivo, con membresía |
| `portafolioInexistenteId` | UUID válido inexistente | Caso 404 |
| `uuidInvalido` | `no-es-un-uuid` | Caso 400 |

Los scripts actualizan `csrfToken` después de `GET /api/auth/csrf` y del login,
`portafolioId` con el primer portafolio autorizado, y los correos de registro
con valores únicos en cada ejecución de `00b`.

## 5. Orden de ejecución

Ejecuta las carpetas en este orden:

```text
00 → 00b → 01 → 02 → 03 → 04 → 99
```

| Orden | Carpeta | Dependencia | Qué comprueba |
|-------|---------|-------------|---------------|
| 1 | `00 · Sin sesión` | Cookies limpias | `401 NO_AUTENTICADO` en `/me`, portafolios y cuentas |
| 2 | `00b · Registro de usuarios` | Sin sesión y antes de `01` | `201` sin sesión, `/me` 401, duplicado 409, validación 400 y 403 sin CSRF |
| 3 | `01 · Autenticación` | Ninguna | CSRF, login del titular y `/me` |
| 4 | `02 · Portafolios y cuentas` | Sesión de `01` | Portafolios autorizados, cuentas enmascaradas, 404 y 400 |
| 5 | `03 · Casos negativos de login` | Ninguna (pide su propio CSRF) | 401 genérico, 400 de validación y 403 sin CSRF |
| 6 | `04 · Usuario sin membresías` | Ninguna (pide su propio CSRF) | Lista vacía y 404 sin revelar el portafolio |
| 7 | `99 · Cierre de sesión` | Sesión de `01` o `04` | CSRF, `204` en logout y `401` posterior en `/me` |

### Paso a paso

1. **`00 · Sin sesión`**
   - Asegúrate de que Postman no tiene cookies de `{{baseUrl}}`.
   - Ejecuta los tres `GET`.
   - Todos deben devolver `401` con `code: "NO_AUTENTICADO"`.

2. **`00b · Registro de usuarios`**
   - Ejecuta `GET /api/auth/csrf (para registro)`; el script genera correos
     únicos para esta ejecución y guarda el token.
   - `POST /api/auth/registro` debe devolver `201` con `id`, `email` y `nombre`,
     sin contraseña, hash ni token de sesión.
   - `GET /api/auth/me tras registro` debe devolver `401`: el registro no inicia
     sesión.
   - Repetir el registro con el mismo correo debe devolver `409` con
     `code: "EMAIL_DUPLICADO"`.
   - La contraseña corta debe devolver `400` con
     `code: "VALIDACION_ENTRADA"`.
   - El registro sin `X-XSRF-TOKEN` debe devolver `403` con
     `code: "ACCESO_DENEGADO"`.
   - Ejecuta esta carpeta sin sesión y antes de `01`. Si ya iniciaste sesión,
     `/me` devolverá `200` y la comprobación fallará.

3. **`01 · Autenticación`**
   - Ejecuta `GET /api/auth/csrf`; el script guarda el token.
   - Ejecuta `POST /api/auth/login`; debe devolver `200` y guardar el token
     rotado.
   - Ejecuta `GET /api/auth/me`; debe devolver el email del titular.

4. **`02 · Portafolios y cuentas`**
   - Ejecuta `GET /api/v1/portafolios`; guarda el primer `id` en
     `{{portafolioId}}`.
   - Ejecuta las consultas de cuentas en orden.
   - El portafolio autorizado debe devolver `200` con `numeroCuenta` enmascarado
     (`****` + últimos cuatro caracteres, o solo asteriscos si el número tiene
     cuatro caracteres o menos).
   - El portafolio ajeno, el inactivo y el inexistente deben devolver `404`.
   - El UUID inválido debe devolver `400` con `code: "ID_INVALIDO"`.

5. **`03 · Casos negativos de login`**
   - El primer request obtiene un CSRF nuevo.
   - Los tres primeros logins deben devolver `401` genérico.
   - El cuerpo vacío y el JSON mal formado deben devolver `400`.
   - El login sin cabecera CSRF debe devolver `403`.
   - Esta carpeta no cierra la sesión del titular.

6. **`04 · Usuario sin membresías`**
   - El primer request obtiene un CSRF nuevo.
   - El login con `sin-acceso@example.com` debe devolver `200`.
   - `GET /api/v1/portafolios` debe devolver `[]`.
   - Las cuentas del portafolio A deben devolver `404`.

7. **`99 · Cierre de sesión`**
   - `GET /api/auth/csrf` obtiene un token válido para el logout.
   - `POST /api/auth/logout` debe devolver `204`.
   - `GET /api/auth/me` debe devolver `401`.

## 6. Ejecución automática con Collection Runner

1. Abre la colección y pulsa `Run`.
2. Selecciona todas las carpetas en el orden mostrado.
3. Desactiva `Delay` o déjalo en `0`.
4. Pulsa `Run Patrimonio · API MVP`.

El Runner ejecuta las carpetas y requests en el orden del árbol. Si alguna
prueba falla, revisa la respuesta y la sección de problemas.

## 7. Repetir la colección

Postman conserva las cookies entre ejecuciones. Si vuelves a ejecutar desde
`00`, puedes tener una sesión antigua y obtener `200` donde esperas `401`.

Opciones:

- Ejecuta primero `99 · Cierre de sesión`.
- O borra las cookies de `{{baseUrl}}` en Postman:
  `Cookies` → selecciona el dominio → `Clear`.
- O usa una instancia nueva de Postman/una colección limpia.

La carpeta `00b` genera un correo nuevo en cada ejecución, así que puedes
repetirla sin chocar con el usuario registrado anteriormente. Los usuarios de
prueba se acumulan en la base desechable.

## 8. Problemas frecuentes

| Síntoma | Causa probable | Solución |
|---------|----------------|----------|
| `401` en todo, incluso tras login | No se guardó la cookie o el login falló | Revisa que el login devolvió `200`; comprueba `PATRIMONIO_SESSION` en Cookies |
| `403` en registro, login o logout | Falta `X-XSRF-TOKEN` o el token no coincide | Ejecuta antes `GET /api/auth/csrf` y no borres la cookie `XSRF-TOKEN` |
| `409` en el registro | El correo ya existe en la base de prueba | Ejecuta `00b` desde su primer request para generar un correo nuevo |
| `404` en el endpoint de cuentas | Perfil sin `local`/`test` o `patrimonio.api.cuentas.expuesta=false` | Activa el perfil y la propiedad; reinicia el backend |
| `404` en el portafolio A | Los datos de prueba no están cargados | Carga `datos-prueba-auth.sql` en una base desechable |
| `400` en el portafolio autorizado | `{{portafolioId}}` no es un UUID válido | Ejecuta `GET /api/v1/portafolios` para actualizarlo |
| `500` en cuentas | Datos persistidos incoherentes | Revisa el log del servidor; no se exponen detalles por HTTP |
| Las pruebas de `00` fallan con `200` | Quedó una sesión anterior | Borra cookies o ejecuta `99` antes |

## 9. Datos de prueba

El script `docs/testing/datos-prueba-auth.sql`:

- Inserta moneda, institución, tipo de cuenta, usuarios, portafolios,
  membresías y cuentas ficticias.
- Comprueba que la tabla `usuario` esté vacía antes de insertar; si ya hay
  usuarios, aborta.
- Usa hashes BCrypt de contraseñas de demostración.
- No debe ejecutarse contra la base local ni contra `patrimonio-postgres`.

Ejemplo con un contenedor efímero:

```powershell
docker run --rm -d --name patrimonio-auth-seed -p 55432:5432 `
  -e POSTGRES_DB=patrimonio_test `
  -e POSTGRES_USER=patrimonio_test `
  -e POSTGRES_PASSWORD=patrimonio_test postgres:16

# Esperar a que acepte conexiones y aplicar esquema y datos:
psql "postgresql://patrimonio_test:patrimonio_test@localhost:55432/patrimonio_test" `
  -f db/esquema.sql -f docs/testing/datos-prueba-auth.sql
```

Después, arranca el backend apuntando a ese contenedor con las variables
`POSTGRES_HOST`, `POSTGRES_PORT`, `POSTGRES_DB`, `POSTGRES_USER` y
`POSTGRES_PASSWORD`, y con el perfil `local` o `test`.

## 10. Notas de seguridad

- La cookie de sesión es `HttpOnly`: JavaScript no puede leerla.
- El token CSRF sí es legible por el cliente; se envía en `X-XSRF-TOKEN`.
- El registro es público pero exige CSRF y no crea sesión: después de registrar
  hay que pasar por el login.
- El registro devuelve solo `id`, `email` y `nombre`; nunca contraseña, hash ni
  token de sesión.
- Un correo duplicado responde `409 EMAIL_DUPLICADO` sin repetir el correo en el
  cuerpo.
- El login rota el token CSRF; el logout lo elimina.
- Los errores de login son genéricos: no distinguen usuario inexistente,
  contraseña incorrecta o cuenta inactiva.
- Los puertos ajenos, inactivos o inexistentes responden `404` para no revelar
  su existencia.
- El número de cuenta se devuelve enmascarado; nunca completo.
