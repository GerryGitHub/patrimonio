# Patrimonio — Base de datos PostgreSQL y backend

Infraestructura de base de datos para el proyecto **Patrimonio**: un único
contenedor PostgreSQL 16 gestionado con Docker Compose, más el esquema inicial de
20 tablas aplicado directamente con `psql`. Incluye además el bootstrap del
backend en Spring Boot sobre Java 25.

Estado actual: **base de datos y backend con seguridad MVP**. La base
PostgreSQL contiene el esquema inicial de 20 tablas y el módulo `cuentas`
expone una consulta de solo lectura de cuentas financieras por portafolio,
protegida por sesión de servidor y autorización por membresía. No hay todavía
frontend ni registro de usuarios, y no se usa ningún sistema de migraciones
(nada de Flyway ni Liquibase). El DDL aplicado está en `db/esquema.sql`; en
`docs/` viven la revisión y las propuestas de evolución (aún **no aplicadas**).

## Estructura

```
patrimonio/
├── docker-compose.yml   # Servicio postgres + volumen persistente
├── .env.example         # Plantilla de variables de entorno (sin credenciales reales)
├── .env                 # Tus credenciales reales (ignorado por git, no se versiona)
├── db/esquema.sql       # DDL aplicado en la base
├── docs/                # Revisión, propuestas, bootstrap y documentación de módulos
├── src/                 # Backend Spring Boot (main, test, integrationTest)
├── build.gradle.kts     # Build Gradle Kotlin DSL
├── gradlew / gradlew.bat
├── .gitignore
└── README.md
```

## Documentación

| Archivo | Contenido |
|---------|-----------|
| `docs/schema-review.md` | Revisión estructural del esquema aplicado |
| `docs/schema-review.sql` | DDL real generado con `pg_dump --schema-only` |
| `docs/er-diagram.md` | Diagrama ER actual y diagrama objetivo propuesto |
| `docs/schema-change-proposal.md` / `.sql` | Propuesta v1 de cambios (no aplicada) |
| `docs/schema-change-proposal-v2.md` / `.sql` | Propuesta v2 corregida (no aplicada) |
| `docs/backend-bootstrap.md` | Stack, perfiles, variables de entorno, pruebas y comandos del backend |
| `docs/cuentas-financieras.md` | Endpoint de consulta de cuentas, arquitectura, mapeo, pruebas y limitación de seguridad |
| `docs/inversiones.md` | Catálogo de instrumentos, posiciones, movimientos, endpoints, compatibilidad, esquema y pruebas de las Entregas 2 y 3 |
| `docs/movimientos.md` | Motor de movimientos, cálculos, idempotencia, concurrencia, reversión, reconstrucción y migración de la Entrega 3 |
| `docs/seguridad-autenticacion.md` | Estrategia de sesión, CSRF, autorización por membresía, pruebas y limitaciones |

> Las propuestas son documentación de diseño: **no se han ejecutado** contra la
> base. El esquema vigente es el de `db/esquema.sql`.

## Backend

El backend usa Java 25, Spring Boot 4.1.2 y Gradle Kotlin DSL. La configuración
completa (perfiles, variables de entorno y aislamiento de pruebas) está en
[`docs/backend-bootstrap.md`](docs/backend-bootstrap.md).

Comandos principales (Windows: `gradlew.bat`; Linux/macOS: `./gradlew`):

```bash
./gradlew compileJava       # compilar
./gradlew test              # unitarias y arquitectura (sin Docker)
./gradlew integrationTest   # integración con PostgreSQL 16 aislado
./gradlew build             # build completo
./gradlew jacocoMergedReport # cobertura combinada (HTML/XML)
```

Las pruebas de integración usan Testcontainers y nunca la base de desarrollo.
Las reglas de arquitectura se verifican con ArchUnit y la cobertura se genera con
JaCoCo. El detalle está en
[`docs/cuentas-financieras.md`](docs/cuentas-financieras.md).

### API actual

Autenticación (disponible en todos los perfiles):

```http
GET  /api/auth/csrf
POST /api/auth/registro
POST /api/auth/login
GET  /api/auth/me
POST /api/auth/logout
```

Portafolios y cuentas (requieren sesión):

```http
GET  /api/v1/monedas
GET  /api/v1/portafolios
POST /api/v1/portafolios
GET  /api/v1/portafolios/{portafolioId}/cuentas
```

Instrumentos y posiciones (requieren sesión y perfil local/test explícito):

```http
GET   /api/v1/instrumentos
POST  /api/v1/instrumentos
GET   /api/v1/instrumentos/{id}
PUT   /api/v1/instrumentos/{id}
PATCH /api/v1/instrumentos/{id}/estado
GET   /api/v1/portafolios/{portafolioId}/posiciones
POST  /api/v1/portafolios/{portafolioId}/posiciones
GET   /api/v1/portafolios/{portafolioId}/posiciones/{id}
PUT   /api/v1/portafolios/{portafolioId}/posiciones/{id}
PATCH /api/v1/portafolios/{portafolioId}/posiciones/{id}/estado
GET   /api/v1/portafolios/{portafolioId}/posiciones/resumen
GET   /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/movimientos
POST  /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/movimientos
GET   /api/v1/portafolios/{portafolioId}/movimientos/{movimientoId}
POST  /api/v1/portafolios/{portafolioId}/movimientos/{movimientoId}/reversion
GET   /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/resumen
POST  /api/v1/portafolios/{portafolioId}/posiciones/{posicionId}/reconstruccion
```

El catálogo de instrumentos es global y compartido. Las posiciones pertenecen a
una cuenta del portafolio y su cantidad/costo es un estado materializado de los
movimientos confirmados. El costo total es de adquisición y no representa valor
de mercado. Los detalles, reglas de compatibilidad, cálculos y cambios de esquema
están en [`docs/inversiones.md`](docs/inversiones.md) y
[`docs/movimientos.md`](docs/movimientos.md).

`GET /api/v1/monedas` devuelve el catálogo real de monedas activas, ordenado por
código. Es la única fuente del selector de moneda base: el cliente no mantiene
una lista duplicada.

`POST /api/v1/portafolios` exige sesión y CSRF, recibe `nombre`, `descripcion`
opcional y `monedaBaseId`, y responde `201` con el portafolio creado y la
cabecera `Location`. El usuario se toma siempre de la sesión, nunca del cuerpo.
La operación crea el portafolio y la membresía `PROPIETARIO` del creador en una
sola transacción. Un nombre o descripción inválidos responden `400
VALIDACION_ENTRADA`; una moneda inexistente o inactiva responde `422
MONEDA_INVALIDA`. El listado incluye `descripcion`, `monedaBaseId` y la moneda
base anidada (`id`, `codigo`, `nombre`, `simbolo`, `decimales`).

El registro (`POST /api/auth/registro`) es público pero exige CSRF, normaliza
nombre y correo, guarda la contraseña como hash BCrypt y responde `201` con la
identidad mínima. **No crea sesión**: el cliente debe iniciar sesión después.
Un correo duplicado responde `409 EMAIL_DUPLICADO`.

La autenticación usa una sesión de servidor con cookie `HttpOnly`
`PATRIMONIO_SESSION` y un token CSRF en la cabecera `X-XSRF-TOKEN`. El login
responde `401` genérico para credenciales incorrectas, usuario inexistente o
usuario inactivo. La autorización se resuelve por membresía: sin membresía,
portafolio inexistente o inactivo, la respuesta es `404`; con membresía sin
permiso de lectura, `403`.

El endpoint de cuentas devuelve el número enmascarado (`****` + últimos cuatro
caracteres). Solo se registra si el perfil `local` o `test` está activo de forma
**explícita** y `patrimonio.api.cuentas.expuesta=true`. Un arranque sin perfil
no lo expone, aunque Spring aplique el perfil por defecto `local` y la propiedad
valga `true` en ese perfil.

Para habilitarlo en desarrollo local:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
# o bien
SPRING_PROFILES_ACTIVE=local ./gradlew bootRun
```

En `prod` el endpoint de cuentas responde `404` y el arranque falla si se fuerza
`patrimonio.api.cuentas.expuesta=true`.

Los detalles de la estrategia, el ciclo de sesión, CSRF, la autorización y las
pruebas están en
[`docs/seguridad-autenticacion.md`](docs/seguridad-autenticacion.md). La
colección Postman, la guía de ejecución y los datos de prueba manuales están en
`docs/testing/`:
[`patrimonio-auth.postman_collection.json`](docs/testing/patrimonio-auth.postman_collection.json),
[`postman-guia.md`](docs/testing/postman-guia.md) y
[`guia-desde-cero.md`](docs/testing/guia-desde-cero.md), que recorre el flujo
completo desde una base vacía (crear usuario, login y datos con SQL).

## Esquema de datos

El esquema se aplicó directamente en la base `patrimonio` (20 tablas, 20 PK,
28 FK, 12 UNIQUE, 18 CHECK, 62 índices). El catálogo mínimo de monedas se carga
con `db/datos-iniciales.sql`, un script idempotente que no crea usuarios,
portafolios ni cuentas:

```bash
psql -v ON_ERROR_STOP=1 -f db/esquema.sql
psql -v ON_ERROR_STOP=1 -f db/datos-iniciales.sql
```

En bases existentes, las Entregas 2 y 3 se aplican sin cambios destructivos:

```bash
psql -v ON_ERROR_STOP=1 -f db/esquema-entrega2.sql
psql -v ON_ERROR_STOP=1 -f db/esquema-entrega3.sql
psql -v ON_ERROR_STOP=1 -f db/datos-iniciales.sql
```

`db/esquema-entrega3.sql` convierte las posiciones existentes en movimientos
`APERTURA` de forma idempotente y conserva sus UUID, cantidades, costos y fechas.

| Grupo | Tablas |
| ----- | ------ |
| Catálogos | `moneda`, `clase_activo`, `tipo_instrumento`, `tipo_cuenta`, `tipo_movimiento`, `tipo_cambio` |
| Usuarios y portafolios | `usuario`, `portafolio`, `membresia` |
| Instituciones y cuentas | `institucion`, `cuenta_financiera` |
| Instrumentos | `instrumento`, `identificador_instrumento` |
| Posiciones y operaciones | `posicion`, `movimiento`, `contrato_inversion`, `historial_tasa`, `pago_rendimiento` |
| Mercado y valoración | `cotizacion`, `valoracion_historica` |

Relaciones principales:

```
usuario 1--N membresia N--1 portafolio
portafolio 1--N cuenta_financiera N--1 institucion
portafolio N--1 moneda (moneda_base)
clase_activo 1--N tipo_instrumento 1--N instrumento 1--N identificador_instrumento
cuenta_financiera 1--N posicion N--1 instrumento
posicion 1--N movimiento N--1 tipo_movimiento
posicion 1--N contrato_inversion 1--N historial_tasa / pago_rendimiento
posicion 1--N valoracion_historica
instrumento 1--N cotizacion
moneda 1--N tipo_cambio (origen y destino)
```

`posicion.contrato_id` y `contrato_inversion.posicion_id` se referencian mutuamente;
la FK de `posicion` se agregó con `ALTER TABLE` después de crear `contrato_inversion`.
La propuesta v2 elimina `posicion.contrato_id` y deja una sola relación (pendiente
de aprobación y aplicación).

Para reaplicar el DDL en una base vacía (destructivo solo si ya hubiera tablas:
el script falla por `CREATE TABLE` duplicado, no sobrescribe nada):

```bash
docker compose cp db/esquema.sql postgres:/tmp/esquema.sql
docker compose exec -T postgres psql -v ON_ERROR_STOP=1 -U patrimonio -d patrimonio -f /tmp/esquema.sql
```

## Requisitos previos

- Docker Desktop (o Docker Engine + plugin Compose v2) en marcha.
- Puerto `5432` libre en el host, o uno alternativo (ver más abajo).
- Cliente de base de datos opcional: DataGrip, DBeaver, `psql`, etc.

Comprobar que Docker responde:

```bash
docker --version
docker compose version
```

## Configuración del `.env`

1. Copia la plantilla:

   ```bash
   cp .env.example .env
   ```

   En PowerShell:

   ```powershell
   Copy-Item .env.example .env
   ```

2. Edita `.env` y define al menos:

   | Variable                 | Descripción                                        | Ejemplo       |
   | ------------------------ | -------------------------------------------------- | ------------- |
   | `POSTGRES_USER`          | Usuario administrador de PostgreSQL                | `patrimonio`  |
   | `POSTGRES_PASSWORD`      | Contraseña del usuario (sin comillas)              | `...`         |
   | `POSTGRES_DB`            | Base de datos creada al inicializar el contenedor  | `patrimonio`  |
   | `POSTGRES_BIND_ADDRESS`  | Interfaz del host donde se publica el puerto       | `127.0.0.1`   |
   | `POSTGRES_PORT`          | Puerto del host                                    | `5432`        |
   | `TZ`                     | Zona horaria del contenedor                        | `UTC`         |

`.env` está en `.gitignore`: no se sube al repositorio. `.env.example` sí se
versiona y no debe contener credenciales reales.

> `POSTGRES_USER`, `POSTGRES_PASSWORD` y `POSTGRES_DB` son **obligatorias**: el
> `docker-compose.yml` falla de forma explícita si faltan.

### Si el puerto 5432 está ocupado

El puerto se publica en `127.0.0.1`, así que solo es accesible desde tu equipo.
Si otro proceso lo usa, identifícalo y cambia el puerto del host:

```powershell
# Windows
Get-NetTCPConnection -LocalPort 5432 | Select-Object LocalAddress, LocalPort, State, OwningProcess
Get-Process -Id <OwningProcess>
```

```bash
# Linux / macOS
ss -ltnp | grep 5432
lsof -i :5432
```

Después ajusta en `.env`:

```env
POSTGRES_PORT=5433
```

El puerto **interno** del contenedor sigue siendo 5432; solo cambia el del host.

## Iniciar PostgreSQL

```bash
# Validar y ver la configuración resuelta (sin arrancar nada)
docker compose config

# Arrancar en segundo plano
docker compose up -d
```

La primera vez se crea el volumen `patrimonio_pgdata` y la base `patrimonio`.
El contenedor informa `healthy` cuando el healthcheck (`pg_isready`) pasa:

```bash
docker compose ps
```

## Consultar los registros

```bash
# Últimas líneas y seguimiento en vivo
docker compose logs -f postgres

# Últimas 100 líneas
docker compose logs --tail 100 postgres
```

Mensajes de referencia:

- `database system is ready to accept connections` → PostgreSQL está listo.
- `database "patrimonio" already exists` en logs antiguos → normal tras reinicios.

## Conectarse desde DataGrip

Crea una conexión **PostgreSQL** con:

| Campo      | Valor                                              |
| ---------- | -------------------------------------------------- |
| Host       | `localhost`                                        |
| Puerto     | `5432` (o el `POSTGRES_PORT` que hayas definido)   |
| Base de datos | `patrimonio` (o tu `POSTGRES_DB`)                |
| Usuario    | el valor de `POSTGRES_USER`                        |
| Contraseña | el valor de `POSTGRES_PASSWORD`                    |

Pasos rápidos:

1. DataGrip → **New Data Source** → **PostgreSQL**.
2. Rellena los campos anteriores (marca *Save password* si lo deseas).
3. Pulsa **Test Connection**; debe responder en verde.
4. **Apply / OK** y explora el esquema `public` (todavía vacío).

Si la conexión falla:

- `Connection refused` → el contenedor no está arriba (`docker compose ps`) o el
  puerto del host es distinto.
- `password authentication failed` → revisa `POSTGRES_USER` / `POSTGRES_PASSWORD`.
  Ojo: las credenciales solo se aplican al **inicializar** el volumen; cambiarlas
  después no afecta a un volumen ya creado.
- `database "..." does not exist` → revisa `POSTGRES_DB`.

## Comprobar que la base existe

Desde el propio contenedor:

```bash
# Lista de bases de datos
docker compose exec postgres psql -U patrimonio -d postgres -c "\l"

# Consulta directa sobre la base del proyecto
docker compose exec postgres psql -U patrimonio -d patrimonio -c "SELECT current_database(), current_user;"
```

Resultado esperado:

```
     db     |  usuario
------------+-----------
 patrimonio | patrimonio
```

Desde el host (si tienes `psql` instalado):

```bash
psql "host=localhost port=5432 dbname=patrimonio user=patrimonio"
```

## Detener sin perder los datos

```bash
# Detiene y elimina el contenedor y la red, pero CONSERVA el volumen
docker compose down
```

Los datos permanecen en el volumen `patrimonio_pgdata`; el siguiente
`docker compose up -d` los recupera tal cual.

Alternativas:

```bash
# Solo parar el contenedor (sigue existiendo)
docker compose stop

# Parar y volver a arrancar
docker compose restart
```

## `docker compose down` vs `docker compose down -v`

| Comando                     | Contenedor | Red | Volumen `patrimonio_pgdata` | Datos |
| --------------------------- | ---------- | --- | --------------------------- | ----- |
| `docker compose down`       | Elimina    | Elimina | **Se conserva**         | **Se conservan** |
| `docker compose down -v`    | Elimina    | Elimina | **Se elimina**          | **Se pierden**   |
| `docker compose stop`       | Conserva   | Conserva | Se conserva            | Se conservan     |

> ⚠️ `docker compose down -v` borra la base de datos de forma permanente.
> Este proyecto **no** elimina volúmenes automáticamente: solo se pierden si
> ejecutas `-v` de forma explícita.

## Solución de problemas

```bash
# Estado del contenedor y del healthcheck
docker compose ps

# Detalle del healthcheck y política de reinicio
docker inspect --format '{{.State.Health.Status}} | {{.HostConfig.RestartPolicy.Name}}' patrimonio-postgres

# Variables de entorno efectivas dentro del contenedor
docker compose exec postgres env | Select-String POSTGRES   # Windows
docker compose exec postgres env | grep POSTGRES            # Linux/macOS
```

Notas de diseño:

- El puerto se publica en `127.0.0.1`, nunca en `0.0.0.0`: no se expone a Internet.
- `restart: unless-stopped`: el contenedor se reinicia solo tras un reinicio del
  equipo o un fallo, salvo que lo detengas manualmente.
- El volumen es **nombrado** (`patrimonio_pgdata`), independiente de otros
  proyectos y de las credenciales de otros contenedores.
