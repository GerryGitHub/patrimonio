# Guía desde cero — base vacía, usuario, login y datos

> Esta guía usa una base PostgreSQL **desechable**. No ejecutes estos pasos
> contra la base local ni contra el contenedor `patrimonio-postgres`.
>
> Estado actual del MVP: **hay registro de usuarios, pero no CRUD de
> portafolios, cuentas o productos**. Los datos de demostración se crean con
> SQL; la API permite registrarse, autenticarse y consultar portafolios y
> cuentas.

## 0. Qué existe y qué no

| Quiero... | ¿Hay endpoint? | Cómo se hace |
|-----------|----------------|--------------|
| Crear un usuario | Sí | `POST /api/auth/registro` (o `INSERT INTO usuario` con hash BCrypt) |
| Iniciar sesión | Sí | `POST /api/auth/login` |
| Consultar mi identidad | Sí | `GET /api/auth/me` |
| Cerrar sesión | Sí | `POST /api/auth/logout` |
| Crear un portafolio | No | `INSERT INTO portafolio` + `membresia` |
| Listar mis portafolios | Sí | `GET /api/v1/portafolios` |
| Crear una cuenta financiera | No | `INSERT INTO cuenta_financiera` |
| Listar cuentas de un portafolio | Sí | `GET /api/v1/portafolios/{id}/cuentas` |
| Crear un “producto” o instrumento | No | No hay API; el esquema tiene catálogos, pero no se exponen |

En este proyecto, lo más parecido a un “producto” es una **cuenta financiera**
dentro de un **portafolio**. La API actual es de solo lectura para esas dos
entidades.

## 1. Requisitos

- Docker Desktop funcionando.
- Java 25 y el wrapper de Gradle del proyecto.
- PowerShell en Windows.
- Estar en `D:\Develop\patrimonio`.

## 2. Crear una base desechable

```powershell
Set-Location D:\Develop\patrimonio

docker rm -f patrimonio-auth-seed 2>$null
docker run --rm -d --name patrimonio-auth-seed -p 55432:5432 `
  -e POSTGRES_DB=patrimonio_test `
  -e POSTGRES_USER=patrimonio_test `
  -e POSTGRES_PASSWORD=patrimonio_test postgres:16
```

Espera unos segundos y comprueba que acepta conexiones:

```powershell
docker exec -it patrimonio-auth-seed pg_isready -U patrimonio_test -d patrimonio_test
```

Debe responder `accepting connections`.

## 3. Aplicar el esquema

Copia el esquema dentro del contenedor y ejecútalo:

```powershell
docker cp "D:\Develop\patrimonio\db\esquema.sql" patrimonio-auth-seed:/tmp/esquema.sql
docker exec -it patrimonio-auth-seed psql -U patrimonio_test -d patrimonio_test -f /tmp/esquema.sql
```

Comprueba que se crearon las tablas:

```powershell
docker exec -it patrimonio-auth-seed psql -U patrimonio_test -d patrimonio_test -c "\dt"
```

## 4. Crear el usuario y los datos

### Opción A: datos de demostración (recomendada)

Este script crea moneda, institución, tipo de cuenta, cuatro usuarios, tres
portafolios, membresías y dos cuentas. Está pensado para una base vacía.

```powershell
docker cp "D:\Develop\patrimonio\docs\testing\datos-prueba-auth.sql" patrimonio-auth-seed:/tmp/datos.sql
docker exec -it patrimonio-auth-seed psql -U patrimonio_test -d patrimonio_test -f /tmp/datos.sql
```

Verifica los usuarios:

```powershell
docker exec -it patrimonio-auth-seed psql -U patrimonio_test -d patrimonio_test `
  -c "SELECT email, activo FROM usuario ORDER BY email;"
```

Deben aparecer:

| Email | Contraseña | Acceso |
|-------|------------|--------|
| `titular@example.com` | `Demo1234!` | `PROPIETARIO` del portafolio A |
| `lector@example.com` | `Lector1234!` | `LECTOR` del portafolio A |
| `sin-acceso@example.com` | `Editor1234!` | Sin membresías |
| `inactivo@example.com` | `Demo1234!` | Inactivo, no puede entrar |

### Opción B: crear un usuario propio

Usa esta opción **solo si no ejecutaste la Opción A**: los catálogos e IDs
chocarían con los del script de demostración. Crea primero los catálogos
mínimos y después tu usuario. El hash de ejemplo corresponde a la contraseña
`Demo1234!`; no guardes contraseñas en claro.

También puedes crear el usuario con `POST /api/auth/registro` (requiere CSRF y
no inicia sesión) y después insertar el portafolio y la membresía con SQL.

```sql
-- Catálogos mínimos
INSERT INTO moneda (id, codigo, nombre, simbolo, decimales, activo)
VALUES ('a0000000-0000-4000-8000-000000000001', 'USD',
        'Dólar estadounidense', 'US$', 2, true);

INSERT INTO institucion (id, nombre, tipo, pais, activo)
VALUES ('b0000000-0000-4000-8000-000000000001',
        'Banco de prueba', 'BANCO', 'ES', true);

INSERT INTO tipo_cuenta (id, nombre, descripcion, activo)
VALUES ('c0000000-0000-4000-8000-000000000001',
        'Corriente', 'Cuenta corriente de prueba', true);

-- Usuario: la contraseña es Demo1234!
INSERT INTO usuario (id, nombre, email, password_hash, activo)
VALUES ('55555555-5555-4555-8555-555555555555',
        'Mi usuario', 'mi@example.com',
        '$2a$10$13EJknBVOgm6UiVUVRFgWuuArAviDADiZr2S1erkROIl.vrAAX.aW',
        true);

-- Portafolio
INSERT INTO portafolio (id, nombre, moneda_base_id, descripcion, activo)
VALUES ('dddddddd-dddd-4ddd-8ddd-ddddddddddd5',
        'Mi portafolio',
        'a0000000-0000-4000-8000-000000000001',
        'Portafolio de prueba', true);

-- Membresía: sin esto el usuario no ve el portafolio
INSERT INTO membresia (id, usuario_id, portafolio_id, rol)
VALUES (gen_random_uuid(),
        '55555555-5555-4555-8555-555555555555',
        'dddddddd-dddd-4ddd-8ddd-ddddddddddd5',
        'PROPIETARIO');

-- Cuenta financiera del portafolio
INSERT INTO cuenta_financiera
    (id, portafolio_id, institucion_id, tipo_cuenta_id, nombre,
     numero_cuenta, moneda_id, activo, fecha_apertura, fecha_cierre)
VALUES (gen_random_uuid(),
        'dddddddd-dddd-4ddd-8ddd-ddddddddddd5',
        'b0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000001',
        'Mi cuenta', 'ES00 1234 5678 9012 3456 7890',
        'a0000000-0000-4000-8000-000000000001',
        true, CURRENT_DATE, NULL);
```

Guarda ese bloque como `mi-usuario.sql` y ejecútalo:

```powershell
docker cp .\mi-usuario.sql patrimonio-auth-seed:/tmp/mi-usuario.sql
docker exec -it patrimonio-auth-seed psql -U patrimonio_test -d patrimonio_test -f /tmp/mi-usuario.sql
```

Si quieres otra contraseña, genera un hash BCrypt nuevo y sustitúyelo en
`password_hash`. No uses texto plano ni un hash de otro algoritmo.

## 5. Arrancar el backend

```powershell
Set-Location D:\Develop\patrimonio

.\gradlew.bat bootRun --args="--spring.profiles.active=local --server.port=18080 --spring.datasource.url=jdbc:postgresql://localhost:55432/patrimonio_test --spring.datasource.username=patrimonio_test --spring.datasource.password=patrimonio_test --patrimonio.api.cuentas.expuesta=true"
```

Espera a ver:

```text
Started PatrimonioApplication
```

La API queda en `http://localhost:18080`. El perfil `local` es el que define el
datasource; no uses `test` para `bootRun`, porque `test` está pensado para
Testcontainers y no define `spring.datasource.url`.

## 6. Login y consultas paso a paso

### Opción A: Postman

1. Importa `docs/testing/patrimonio-auth.postman_collection.json`.
2. Cambia `baseUrl` a `http://localhost:18080`.
3. Ejecuta las carpetas en este orden:

   ```text
   00 → 00b → 01 → 02 → 03 → 04 → 99
   ```

4. `00b · Registro de usuarios` crea un usuario nuevo con un correo único y
   comprueba que el registro no inicia sesión. Es opcional si solo quieres usar
   los usuarios de demostración.
5. En `01 · Autenticación`, el login usa:
   - usuario: `titular@example.com`
   - contraseña: `Demo1234!`

La colección obtiene el CSRF, guarda la cookie de sesión y rellena
`csrfToken` y `portafolioId` automáticamente.

### Opción B: PowerShell

```powershell
$base = "http://localhost:18080"
$session = New-Object Microsoft.PowerShell.Commands.WebRequestSession

# 1. Obtener CSRF
$csrf = Invoke-RestMethod -Uri "$base/api/auth/csrf" -WebSession $session

# 2. Login (la cookie de sesión queda en $session)
$login = Invoke-RestMethod -Method Post -Uri "$base/api/auth/login" `
  -WebSession $session `
  -ContentType "application/json" `
  -Headers @{ "X-XSRF-TOKEN" = $csrf.token } `
  -Body '{"usuario":"titular@example.com","password":"Demo1234!"}'

# 3. Identidad
$me = Invoke-RestMethod -Uri "$base/api/auth/me" -WebSession $session

# 4. Portafolios autorizados
$portafolios = Invoke-RestMethod -Uri "$base/api/v1/portafolios" -WebSession $session

# 5. Cuentas del primer portafolio
$cuentas = Invoke-RestMethod -Uri "$base/api/v1/portafolios/$($portafolios[0].id)/cuentas" -WebSession $session

$me
$portafolios
$cuentas

# 6. Logout con el token rotado por el login
Invoke-RestMethod -Method Post -Uri "$base/api/auth/logout" `
  -WebSession $session `
  -Headers @{ "X-XSRF-TOKEN" = $login.csrfToken }
```

El login devuelve `csrfToken`; úsalo para el logout. Los `GET` no necesitan la
cabecera CSRF, solo la cookie de sesión que PowerShell guarda en `$session`.

## 7. Agregar un portafolio o una cuenta después

No hay endpoint `POST`; se hace con SQL y luego se consulta por la API.

### Portafolio nuevo para un usuario existente

```sql
INSERT INTO portafolio (id, nombre, moneda_base_id, descripcion, activo)
VALUES ('eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee6',
        'Portafolio nuevo',
        'a0000000-0000-4000-8000-000000000001',
        'Creado por SQL', true);

INSERT INTO membresia (id, usuario_id, portafolio_id, rol)
VALUES (gen_random_uuid(),
        '11111111-1111-4111-8111-111111111111',
        'eeeeeeee-eeee-4eee-8eee-eeeeeeeeeee6',
        'PROPIETARIO');
```

Después de insertarlo, `GET /api/v1/portafolios` lo devolverá para el titular.

### Cuenta nueva dentro de un portafolio

```sql
INSERT INTO cuenta_financiera
    (id, portafolio_id, institucion_id, tipo_cuenta_id, nombre,
     numero_cuenta, moneda_id, activo, fecha_apertura, fecha_cierre)
VALUES (gen_random_uuid(),
        'aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaa1',
        'b0000000-0000-4000-8000-000000000001',
        'c0000000-0000-4000-8000-000000000001',
        'Cuenta nueva', 'ES12 3456 7890 1234 5678 9012',
        'a0000000-0000-4000-8000-000000000001',
        true, CURRENT_DATE, NULL);
```

La API la devolverá enmascarada: `****9012`.

### “Producto” o instrumento

No existe endpoint de productos ni de instrumentos en este MVP. El esquema
tiene catálogos financieros, pero no hay caso de uso ni controlador que los
exponga. Si necesitas ese flujo, hay que implementarlo; no se puede crear por
HTTP con lo que existe hoy.

## 8. Verificar y limpiar

Comprobar datos:

```powershell
docker exec -it patrimonio-auth-seed psql -U patrimonio_test -d patrimonio_test `
  -c "SELECT p.nombre, u.email, m.rol FROM portafolio p JOIN membresia m ON m.portafolio_id = p.id JOIN usuario u ON u.id = m.usuario_id ORDER BY p.nombre, u.email;"
```

Al terminar, detén y elimina el contenedor desechable:

```powershell
docker rm -f patrimonio-auth-seed
```

El contenedor `patrimonio-postgres` no se toca en ningún paso.

## 9. Problemas frecuentes

| Síntoma | Causa | Solución |
|---------|-------|----------|
| `Failed to configure a DataSource: 'url' attribute is not specified` | Perfil `test` activo o sin perfil `local` | Arranca con `--spring.profiles.active=local` y el datasource explícito |
| `401` en login | Usuario o contraseña incorrectos, o usuario inactivo | Usa `titular@example.com` / `Demo1234!` |
| `403` en login/logout | Falta el token CSRF | Ejecuta antes `GET /api/auth/csrf` y envía `X-XSRF-TOKEN` |
| `404` en cuentas | El portafolio no existe, está inactivo o no tienes membresía | Revisa `membresia` y que el portafolio esté activo |
| `[]` en portafolios | El usuario no tiene membresías | Inserta una fila en `membresia` |
| El endpoint de cuentas no existe | Perfil sin `local`/`test` o `patrimonio.api.cuentas.expuesta=false` | Usa el perfil `local` y la propiedad `true` |
| `password authentication failed` | Credenciales del datasource distintas | Usa `patrimonio_test` / `patrimonio_test` y el puerto `55432` |
| El contenedor no arranca | Puerto `55432` ocupado | Cambia el puerto y ajusta la URL del datasource |

## 10. Resumen del orden

```text
1. Crear PostgreSQL desechable
2. Aplicar db/esquema.sql
3. Aplicar docs/testing/datos-prueba-auth.sql
4. Arrancar el backend con perfil local y datasource al contenedor
5. GET /api/auth/csrf
6. POST /api/auth/registro (opcional; no crea sesión)
7. POST /api/auth/login
8. GET /api/auth/me
9. GET /api/v1/portafolios
10. GET /api/v1/portafolios/{id}/cuentas
11. POST /api/auth/logout
```

El registro de usuarios ya está disponible por HTTP, pero no crea sesión:
después hay que hacer login. Los portafolios, las cuentas y los productos no
tienen CRUD por HTTP en esta fase; se crean con SQL.
