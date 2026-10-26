# Patrimonio — Límites de módulos

> Documento de diseño. No contiene código ejecutable ni modifica la base de
> datos. Fecha: 2026-10-23.
>
> Define qué módulos existen, qué tablas posee cada uno, qué dependencias están
> permitidas y cómo se protegen los límites.

## 1. Criterios para definir módulos

Un módulo no es una tabla. Un módulo es un **límite de responsabilidad, lenguaje
y ciclo de vida**. Se justifica cuando cumple varios de estos criterios:

1. Tiene un conjunto de reglas de negocio propias.
2. Tiene un lenguaje ubicuo distinto (posiciones vs. contratos vs. mercado).
3. Sus datos cambian por actores/procesos distintos.
4. Puede evolucionar sin cambiar el resto.
5. Tiene casos de uso propios (no solo CRUD de una tabla).
6. Su consistencia se puede mantener en una transacción local.

No se crea un módulo por:

- una tabla aislada sin reglas;
- una relación muchos-a-muchos;
- un catálogo pequeño que solo se lee;
- una pantalla o un endpoint;
- una moda arquitectónica.

## 2. Catálogo de módulos

### 2.1 `catalogos` — datos de referencia

| Aspecto | Detalle |
|---------|---------|
| Tablas | `moneda`, `clase_activo`, `tipo_instrumento`, `tipo_cuenta`, `tipo_movimiento` |
| Agregados | `Moneda`, `ClaseActivo`, `TipoInstrumento`, `TipoCuenta`, `TipoMovimiento` |
| Responsabilidad | Mantener los catálogos que clasifican monedas, activos, instrumentos, cuentas y movimientos |
| Casos de uso | Listar/crear/actualizar/desactivar cada catálogo |
| API pública | `MonedaQueryPort`, `ClaseActivoQueryPort`, `TipoInstrumentoQueryPort`, `TipoCuentaQueryPort`, `TipoMovimientoQueryPort` |
| Dependencias | Ninguna de negocio; solo `shared` |
| Notas | `tipo_instrumento` referencia `clase_activo` dentro del mismo módulo. `tipo_movimiento.categoria` es texto libre; su semántica está pendiente. |

### 2.2 `acceso` — identidad, portafolios y roles

| Aspecto | Detalle |
|---------|---------|
| Tablas | `usuario`, `portafolio`, `membresia` |
| Agregados | `Usuario`, `Portafolio`, `Membresia` |
| Responsabilidad | Quién es el usuario, qué portafolios existen y qué rol tiene cada usuario en cada portafolio |
| Casos de uso | Alta/actualización/baja de usuario; alta/actualización de portafolio; asignar/cambiar/revocar membresía |
| API pública | `PortafolioQueryPort`, `AccesoQueryPort` |
| Dependencias | `catalogos` (solo consulta de moneda) |
| Notas | `password_hash` existe, pero autenticación/autorización están pendientes. `membresia.rol` tiene CHECK `PROPIETARIO/ADMINISTRADOR/EDITOR/LECTOR`; las reglas de qué puede hacer cada rol no están definidas. |

**Por qué no separar `usuario` de `portafolio` ahora:** son tres tablas con un
ciclo de vida acoplado (una membresía une usuario y portafolio) y el equipo es
pequeño. Si la autenticación crece (MFA, sesiones, IdP), `identidad` se puede
separar de `portafolios` sin cambiar el resto.

### 2.3 `cuentas` — instituciones y cuentas financieras

| Aspecto | Detalle |
|---------|---------|
| Tablas | `institucion`, `cuenta_financiera` |
| Agregados | `Institucion`, `CuentaFinanciera` |
| Responsabilidad | Dónde está el dinero: institución, tipo de cuenta, moneda, número, fechas |
| Casos de uso | Alta/actualización de institución; abrir/cerrar/consultar/listar cuentas |
| API pública | `CuentaQueryPort`, `InstitucionQueryPort` |
| Dependencias | `acceso` (portafolio), `catalogos` (moneda, tipo de cuenta) |
| Notas | `cuenta_financiera` tiene UNIQUE parcial `(institucion_id, numero_cuenta)` cuando el número no es NULL. `fecha_cierre` no se valida contra `activo`; la regla de negocio está pendiente. |

### 2.4 `instrumentos` — maestro de instrumentos

| Aspecto | Detalle |
|---------|---------|
| Tablas | `instrumento`, `identificador_instrumento` |
| Agregados | `Instrumento` (con `IdentificadorInstrumento` como entidad interna) |
| Responsabilidad | Identidad del activo: tipo, símbolo, emisor, moneda, identificadores (ISIN, ticker, etc.) |
| Casos de uso | Alta/actualización de instrumento; agregar identificador; buscar por símbolo/identificador |
| API pública | `InstrumentoQueryPort` |
| Dependencias | `catalogos` (tipo de instrumento, moneda) |
| Notas | UNIQUE parcial `(simbolo, moneda_id)` cuando el símbolo no es NULL; UNIQUE `(instrumento_id, tipo)` para identificadores. `identificador_instrumento.tipo` es texto libre. |

### 2.5 `posiciones` — tenencias y movimientos

| Aspecto | Detalle |
|---------|---------|
| Tablas | `posicion`, `movimiento` |
| Agregados | `Posicion`, `Movimiento` |
| Responsabilidad | Qué tiene cada cuenta, cuánto y a qué costo; qué movimientos explican el estado |
| Casos de uso | Abrir/cerrar/consultar posición; registrar movimiento; listar movimientos |
| API pública | `PosicionQueryPort` |
| Dependencias | `cuentas`, `instrumentos`, `catalogos`; `mercado` solo si se implementa multidivisa |
| Notas | `posicion.contrato_id` existe pero se ignora (relación duplicada; v2 propone eliminarla). `movimiento` es un agregado separado, no una colección dentro de `Posicion`. El método de coste y los signos están pendientes. |

**Por qué movimientos no es un módulo aparte:** un movimiento no tiene sentido
sin la posición y su registro actualiza el estado de la posición en la misma
transacción. Separarlo obligaría a una coordinación transaccional artificial.

### 2.6 `contratos` — contratos, tasas y pagos

| Aspecto | Detalle |
|---------|---------|
| Tablas | `contrato_inversion`, `historial_tasa`, `pago_rendimiento` |
| Agregados | `ContratoInversion` (con `HistorialTasa` y `PagoRendimiento` como entidades internas) |
| Responsabilidad | Condiciones contractuales, evolución de tasas y pagos de rendimiento |
| Casos de uso | Crear/actualizar/cerrar contrato; registrar tasa; registrar pago; consultar contrato |
| API pública | Ninguna inicialmente; se añadirá si otro módulo necesita consultar contratos |
| Dependencias | `posiciones` (solo consulta), `catalogos` (moneda de pagos) |
| Notas | `contrato_inversion.posicion_id` es la única relación vigente con posición. `historial_tasa` no tiene restricción de no-solape en el esquema actual. `pago_rendimiento` no tiene clave natural única. |

### 2.7 `mercado` — cotizaciones y tipos de cambio

| Aspecto | Detalle |
|---------|---------|
| Tablas | `cotizacion`, `tipo_cambio` |
| Agregados | `Cotizacion`, `TipoCambio` |
| Responsabilidad | Precios de mercado y tasas de conversión, con fecha y fuente |
| Casos de uso | Registrar/importar cotizaciones; registrar/importar tipos de cambio; convertir moneda |
| API pública | `CotizacionQueryPort`, `TipoCambioQueryPort` |
| Dependencias | `instrumentos` (solo consulta), `catalogos` (moneda) |
| Notas | UNIQUE `(instrumento_id, fecha, moneda_id)` y `(origen, destino, fecha)`. `fuente` es opcional. No se inventan proveedores ni series. |

### 2.8 `valoraciones` — snapshots de valoración

| Aspecto | Detalle |
|---------|---------|
| Tablas | `valoracion_historica` |
| Agregados | `ValoracionHistorica` |
| Responsabilidad | Valor económico y costo de una posición en una fecha, en su moneda |
| Casos de uso | Valorar posición; valorar portafolio; consultar/listar valoraciones |
| API pública | Ninguna inicialmente |
| Dependencias | `posiciones`, `mercado`, `catalogos` (solo consulta) |
| Notas | UNIQUE `(posicion_id, fecha)`: una valoración por posición y día. En el esquema actual los importes admiten NULL; el dominio propone exigirlos (decisión pendiente). `ganancia_perdida` se guarda como columna, no es generada en el esquema actual. |

**Por qué `valoraciones` es un módulo propio:** es un proceso de negocio
(calcular y persistir un snapshot) con su propio ciclo de vida, no una simple
tabla de mercado. Si en el futuro se queda sin casos de uso propios, la fusión
natural es con `mercado`.

## 3. Propiedad de las tablas

Cada tabla tiene **un único módulo propietario** (el que puede escribir). Los
demás solo leen a través de `api`.

| # | Tabla | Módulo propietario | Agregado | ¿Otros módulos escriben? |
|---|-------|--------------------|----------|--------------------------|
| 1 | `usuario` | `acceso` | `Usuario` | No |
| 2 | `portafolio` | `acceso` | `Portafolio` | No |
| 3 | `membresia` | `acceso` | `Membresia` | No |
| 4 | `institucion` | `cuentas` | `Institucion` | No |
| 5 | `cuenta_financiera` | `cuentas` | `CuentaFinanciera` | No |
| 6 | `moneda` | `catalogos` | `Moneda` | No |
| 7 | `clase_activo` | `catalogos` | `ClaseActivo` | No |
| 8 | `tipo_instrumento` | `catalogos` | `TipoInstrumento` | No |
| 9 | `tipo_cuenta` | `catalogos` | `TipoCuenta` | No |
| 10 | `tipo_movimiento` | `catalogos` | `TipoMovimiento` | No |
| 11 | `tipo_cambio` | `mercado` | `TipoCambio` | No |
| 12 | `instrumento` | `instrumentos` | `Instrumento` | No |
| 13 | `identificador_instrumento` | `instrumentos` | `Instrumento` (entidad interna) | No |
| 14 | `posicion` | `posiciones` | `Posicion` | No |
| 15 | `movimiento` | `posiciones` | `Movimiento` | No |
| 16 | `contrato_inversion` | `contratos` | `ContratoInversion` | No |
| 17 | `historial_tasa` | `contratos` | `ContratoInversion` (entidad interna) | No |
| 18 | `pago_rendimiento` | `contratos` | `ContratoInversion` (entidad interna) | No |
| 19 | `cotizacion` | `mercado` | `Cotizacion` | No |
| 20 | `valoracion_historica` | `valoraciones` | `ValoracionHistorica` | No |

Total: **20 tablas, 8 propietarios**. Ninguna tabla es compartida para escritura.

## 4. Matriz de dependencias

Leyenda:

- **Q**: el módulo origen puede importar **solo** `<destino>.api` (puertos de
  consulta y DTOs). No puede importar `domain`, `application` ni `adapter` del
  destino.
- **—**: no hay dependencia permitida.
- **Q\***: dependencia opcional, solo cuando se implemente la capacidad
  multidivisa.

| Desde \ Hacia | `catalogos` | `acceso` | `cuentas` | `instrumentos` | `posiciones` | `contratos` | `mercado` | `valoraciones` |
|---------------|-------------|----------|-----------|----------------|--------------|-------------|-----------|----------------|
| `catalogos` | — | — | — | — | — | — | — | — |
| `acceso` | Q | — | — | — | — | — | — | — |
| `cuentas` | Q | Q | — | — | — | — | — | — |
| `instrumentos` | Q | — | — | — | — | — | — | — |
| `posiciones` | Q | — | Q | Q | — | — | Q\* | — |
| `contratos` | Q | — | — | — | Q | — | — | — |
| `mercado` | Q | — | — | Q | — | — | — | — |
| `valoraciones` | Q | — | — | — | Q | — | Q | — |

El grafo es un **DAG** (sin ciclos). En particular:

- `catalogos` no depende de nadie.
- `posiciones` no depende de `contratos`; es al revés.
- `mercado` no depende de `valoraciones`; es al revés.
- `cuentas` no depende de `posiciones`; es al revés.
- `instrumentos` no depende de `posiciones` ni de `mercado`.

## 5. Reglas explícitas

### R1 — Propiedad única de escritura

Cada tabla tiene un solo módulo que la escribe. Ningún otro módulo ejecuta
`INSERT`, `UPDATE` o `DELETE` sobre ella, ni siquiera a través de un repositorio
compartido.

### R2 — Prohibido leer tablas ajenas

Un módulo no puede tener entidades JPA ni repositorios de tablas de otro módulo.
La única vía de lectura es un puerto de `<módulo>.api`.

### R3 — Sin asociaciones JPA entre módulos

Las entidades JPA solo pueden tener asociaciones con entidades del **mismo
módulo** y, preferiblemente, dentro del mismo agregado. Las referencias
externas se guardan como UUID (`PosicionId`, `MonedaId`, etc.), no como
`@ManyToOne`.

### R4 — Solo `api` es público

Otros módulos solo importan `com.patrimonio.<módulo>.api.*`. Los paquetes
`domain`, `application` y `adapter` son internos. No hay excepciones “porque es
más rápido”.

### R5 — Dominio puro

El dominio de un módulo solo depende de su propio dominio y de
`shared.domain`. No depende de Spring, JPA, Hibernate, Jackson, Jakarta
Validation ni de otro módulo.

### R6 — Aplicación hacia dentro

La aplicación depende de su dominio y de sus puertos. Puede usar
`@Transactional` de Spring como única anotación de framework; no usa
`EntityManager`, `JpaRepository` ni DTOs web.

### R7 — Adaptadores hacia dentro

Los adaptadores dependen de la aplicación/dominio de su módulo. El dominio y la
aplicación no conocen adaptadores.

### R8 — Sin ciclos

El grafo de dependencias debe ser acíclico. Si aparece un ciclo, se rediseña el
límite o se introduce un evento/orquestador; no se resuelve con una dependencia
bidireccional.

### R9 — `catalogos` y `mercado` son proveedores

`catalogos` y `mercado` son módulos de datos de referencia/mercado. Otros
módulos los leen, pero ellos no leen datos transaccionales (posiciones,
contratos, valoraciones). `mercado` sí puede leer el maestro de instrumentos y
los catálogos que necesita.

### R10 — Comunicación síncrona en proceso

Las llamadas entre módulos son llamadas Java en el mismo proceso. No hay HTTP
interno, broker, cola ni bus. Un puerto de consulta se implementa como un bean
Spring más.

### R11 — Transacciones de un solo módulo para escritura

Un caso de uso puede leer de varios módulos, pero **solo escribe en su módulo**.
No hay transacciones que abarquen escrituras de dos módulos. Si un flujo lo
necesita, se diseña un orquestador/evento y se documenta; no se comparte el
`EntityManager`.

### R12 — Sin abstracciones genéricas

No hay `BaseEntity`, `BaseRepository<T, ID>`, `AbstractCrudService<T>`,
`GenericMapper<E, D>` ni `CatalogoService` que sirva para todo. Cada agregado
tiene su puerto y su mapper explícitos.

### R13 — Controladores solo hablan con puertos de entrada

Un controlador no inyecta repositorios, `EntityManager`, mappers de persistencia
ni entidades. Solo llama a `port.in`.

### R14 — Shared kernel mínimo

`shared` contiene IDs tipados, value objects comunes, paginación y excepciones
base. No contiene entidades de negocio, repositorios, servicios de un módulo ni
lógica que dependa de una tabla concreta.

### R15 — Criterio para crear un módulo nuevo

Debe tener: tablas propias, lenguaje propio, al menos dos casos de uso con
reglas, y un ciclo de vida distinto. Si no, se queda dentro del módulo
existente. Crear un módulo es una decisión documentada, no un reflejo.

### R16 — Los límites se verifican

Se aplican reglas ArchUnit (o revisión manual equivalente) en CI. Un límite que
no se verifica se rompe con el tiempo.

## 6. Contratos entre módulos

### 6.1 Contenido de `api`

| Elemento | Permitido | Prohibido |
|----------|-----------|-----------|
| Puertos de consulta | `PortafolioQueryPort`, `CuentaQueryPort`, `TipoCambioQueryPort` | Repositorios de escritura |
| DTOs | `record` inmutables con campos explícitos | Entidades JPA, agregados, `Page<Entity>` |
| Anotaciones | Ninguna o solo las estándar de Java | `@Entity`, `@Transactional`, `@JsonProperty` |
| Dependencias | `shared` | `adapter`, `application.service` del proveedor |

### 6.2 Semántica de los puertos de consulta

- Son de **solo lectura**; no cambian estado ni publican eventos.
- Devuelven `Optional<DTO>` cuando “no encontrado” es un resultado normal.
- Lanzan excepción solo ante fallos de infraestructura, no ante ausencia de dato.
- No cargan relaciones perezosas: el DTO se construye completo dentro de la
  consulta.
- No aceptan entidades ni agregados como parámetro; solo IDs y filtros simples.
- Son estables: añadir un campo es compatible; renombrar/eliminar es un cambio
  de contrato que requiere revisar consumidores.

### 6.3 Ejemplo de colaboración

`RegistrarMovimientoUseCase` (módulo `posiciones`) necesita validar la cuenta y
el instrumento:

1. Llama a `CuentaQueryPort.buscar(cuentaId)` (módulo `cuentas`).
2. Llama a `InstrumentoQueryPort.buscar(instrumentoId)` (módulo `instrumentos`).
3. Llama a `TipoMovimientoQueryPort.buscar(tipoMovimientoId)` (módulo
   `catalogos`).
4. Escribe `posicion` y `movimiento` en su propia transacción.

No accede a `cuenta_financiera` ni a `instrumento` directamente.

## 7. Transacciones entre módulos

| Flujo | Lee de | Escribe en | ¿Una transacción? |
|-------|--------|------------|-------------------|
| Crear portafolio | `catalogos` (moneda) | `acceso` | Sí, solo escribe `acceso` |
| Abrir cuenta | `acceso`, `catalogos` | `cuentas` | Sí, solo escribe `cuentas` |
| Crear instrumento | `catalogos` | `instrumentos` | Sí, solo escribe `instrumentos` |
| Abrir posición | `cuentas`, `instrumentos`, `catalogos` | `posiciones` | Sí, solo escribe `posiciones` |
| Registrar movimiento | `cuentas`, `instrumentos`, `catalogos` | `posiciones` | Sí, solo escribe `posiciones` |
| Crear contrato | `posiciones` | `contratos` | Sí, solo escribe `contratos` |
| Registrar cotización | `instrumentos`, `catalogos` | `mercado` | Sí, solo escribe `mercado` |
| Valorar posición | `posiciones`, `mercado`, `catalogos` | `valoraciones` | Sí, solo escribe `valoraciones` |
| Valorar portafolio | `posiciones`, `mercado` | `valoraciones` | Una transacción por posición; resultado parcial |

Regla: si un flujo necesita escribir en dos módulos, **no** se amplía la
transacción. Se diseña un caso de uso orquestador con eventos/compensación o se
revisa si los módulos deberían ser uno.

## 8. Eventos de dominio

**No se usan en la primera iteración.** No hay un requisito de efecto secundario
asíncrono que los justifique, y añadirlos ahora sería una abstracción
innecesaria.

Si más adelante se necesitan:

- Se definen en `<módulo>.domain.event` como hechos en pasado
  (`MovimientoRegistrado`, `ContratoCreado`).
- Se publican después del commit (`@TransactionalEventListener` en fase
  `AFTER_COMMIT`) para efectos no críticos.
- Son **en proceso**: no hay broker ni outbox.
- No se usan para consistencia fuerte; si el efecto es obligatorio, se hace en
  la misma transacción y en el mismo módulo.
- Un fallo del listener no revierte la transacción original; debe ser
  idempotente y observable.

## 9. Datos y reportería

- No hay vistas ni joins entre tablas de módulos distintos en el camino
  transaccional.
- Si se necesita reportería, se crea un **read model** dedicado (esquema/tablas
  propias o consultas de solo lectura) alimentado por lotes/eventos. No se
  rompen los límites del OLTP.
- No se comparten entidades JPA entre módulos para “reutilizar” consultas.
- No se crea una tabla `reporte_*` sin una propuesta de esquema aprobada.

## 10. Cómo se hacen cumplir los límites

### 10.1 Reglas ArchUnit propuestas

| # | Regla |
|---|-------|
| A1 | `..domain..` no depende de `org.springframework..`, `jakarta.persistence..`, `org.hibernate..`, `com.fasterxml.jackson..`, `jakarta.validation..` |
| A2 | `..adapter.in.web..` no depende de `..adapter.out.persistence..` ni de `..application.port.out..` |
| A3 | `..adapter.out.persistence..` no depende de `..adapter.in.web..` |
| A4 | Un módulo no importa `..<otro>.domain..`, `..<otro>.application..` ni `..<otro>.adapter..`; solo `..<otro>.api..` |
| A5 | No hay ciclos entre paquetes de módulos |
| A6 | Los controladores solo llaman a `..application.port.in..` |
| A7 | `..api..` no depende de `..adapter..` ni de `..application.service..` |
| A8 | `shared` no depende de ningún módulo de negocio |
| A9 | Ningún `@Entity` fuera de `..adapter.out.persistence.entity..` |
| A10 | Ningún `@Transactional` en `..adapter.in.web..` ni en `..adapter.out.persistence..` |

ArchUnit no está administrada por el BOM de Spring Boot. Si se adopta, se fija
una versión estable y se justifica. Si no se adopta, se usa una checklist de
revisión de PR equivalente; el límite sigue siendo obligatorio.

### 10.2 Separación futura en subproyectos Gradle

Si el proyecto crece, cada módulo puede ser un subproyecto `java-library`:

- `<módulo>-api`: puertos y DTOs públicos; depende de `shared`.
- `<módulo>-impl`: dominio, aplicación y adaptadores; depende de su `api` y de
  las `api` de los módulos permitidos.
- Los consumidores dependen de `<módulo>-api` con `api`, no de `impl`.

Esto convierte las reglas de paquetes en reglas de compilación. No se hace ahora
porque 8 subproyectos para 20 tablas sería desproporcionado.

## 11. Evolución de los módulos

| Situación | Acción |
|-----------|--------|
| Un módulo supera ~15 casos de uso o varios equipos lo tocan | Evaluar separación en subproyecto y, si hay autonomía real, en servicio (no antes) |
| `valoraciones` sigue con un solo caso de uso | Fusionar con `mercado`; mantener el lenguaje de valoración como subpaquete |
| La autenticación crece (MFA, sesiones, IdP) | Separar `identidad` de `portafolios` dentro de `acceso` |
| Aparece reportería pesada | Crear read model; no unir tablas de módulos |
| Aparece un flujo que escribe en dos módulos | Orquestador/evento/compensación; no transacción compartida |
| Se aprueba la propuesta v2 | Revisar límites: `contrato_origen_id` sigue en `contratos`; `movimiento_origen_id` en `posiciones`; `plan_ppr` sería un módulo o subpaquete nuevo (opcional) |

## 12. Decisiones pendientes

| # | Decisión | Impacto |
|---|----------|---------|
| 1 | Confirmar los 8 módulos y sus nombres | Estructura de paquetes y equipos |
| 2 | Confirmar `acceso` agrupando usuario + portafolio + membresía | Si se separa, aparecen dos módulos y una dependencia más |
| 3 | Confirmar `valoraciones` como módulo propio | Si se fusiona, se reduce un módulo y una dependencia |
| 4 | Confirmar `posiciones` como dueño de `movimiento` | Si se separa, se necesita coordinación transaccional |
| 5 | Confirmar `mercado` como dueño de cotizaciones y tipos de cambio | Si se separan, `posiciones`/`valoraciones` dependen de dos módulos |
| 6 | Confirmar el patrón de `api` propiedad del proveedor | Alternativa: puertos propiedad del consumidor (más acoplamiento inverso) |
| 7 | Confirmar que no habrá eventos en la primera iteración | Si se usan, definir publicación after-commit e idempotencia |
| 8 | Aprobar ArchUnit y su versión | Si no, revisión manual |
| 9 | Definir si `pago_rendimiento` necesita idempotencia/clave natural | Puede requerir cambio de esquema |
| 10 | Definir semántica de `tipo_movimiento.categoria` y método de coste | Afecta `posiciones` y reportes |
