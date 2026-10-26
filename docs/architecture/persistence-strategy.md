# Patrimonio — Estrategia de persistencia

> Documento de diseño. No contiene código ejecutable, no ejecuta SQL y no
> modifica el esquema, los datos, Docker Compose, contenedores ni volúmenes.
> Fecha: 2026-10-23.
>
> El esquema PostgreSQL existente es la **fuente de verdad**. Las propuestas v1 y
> v2 **no están aplicadas** y no se asumen en el mapeo.

## 1. Principio rector

1. El esquema aplicado (`db/esquema.sql`, verificado en
   `docs/schema-review.sql`) define tablas, columnas, tipos, claves, índices y
   restricciones.
2. Hibernate **solo mapea** ese esquema. No lo crea, no lo actualiza y no lo
   elimina.
3. No hay Flyway, Liquibase ni equivalente. Los cambios de esquema son
   propuestas revisadas y aplicadas manualmente.
4. El dominio no conoce JPA. Las entidades JPA son un detalle del adaptador de
   salida.
5. No se inventan tablas, columnas, relaciones ni restricciones. Si una
   capacidad no existe en el esquema actual, se documenta como pendiente.

## 2. Estado actual y proceso de cambio

### 2.1 Estado

| Dato | Valor |
|------|-------|
| Motor | PostgreSQL 16.11 |
| Base / esquema | `patrimonio` / `public` |
| Tablas | 20 |
| Filas | 0 |
| Restricciones | 78 (20 PK, 28 FK, 12 UNIQUE, 18 CHECK) |
| Índices | 62 |
| Extensiones | Solo `plpgsql` |
| Migraciones | Ninguna |
| DDL de referencia | `db/esquema.sql` |

### 2.2 Proceso de cambio (sin migraciones automáticas)

1. Se documenta la propuesta en `docs/schema-change-proposal*.md` / `.sql`.
2. Se revisa y aprueba (incluye impacto en mapeo, pruebas y datos).
3. Se hace respaldo: `pg_dump -Fc` de la base.
4. Se aplica primero en una base de ensayo con
   `psql -v ON_ERROR_STOP=1`.
5. Se ejecutan las pruebas de esquema, repositorio e integración.
6. Se aplica en producción con un usuario administrador, en ventana controlada.
7. Se actualiza `db/esquema.sql` y, si aplica, `docs/schema-review.sql`; se
   registra la huella del `pg_dump`.
8. La aplicación se despliega después; nunca es la aplicación quien cambia el
   esquema.

**Prohibido:** `ddl-auto=create`, `create-drop`, `update`; `spring.sql.init` en
producción; scripts de migración automáticos; `DROP`/`ALTER` desde la
aplicación.

## 3. Configuración de Hibernate/JPA

### 3.1 Comparación de `ddl-auto`

| Valor | Qué hace | Ventajas | Riesgos | ¿Cuándo usar? |
|-------|----------|----------|---------|----------------|
| `none` | No hace nada; no valida | Arranque resistente; no requiere leer metadatos; el esquema se administra fuera | Un desajuste se descubre en la primera consulta; drift silencioso | **Producción**, con validación previa en CI/staging |
| `validate` | Valida que tablas/columnas mapeadas existan y los tipos sean compatibles; no modifica nada | Detecta drift al arrancar; útil en desarrollo y pruebas | Coste de arranque; requiere permisos de metadatos; no valida índices, FK, UNIQUE, CHECK ni defaults; puede fallar por tipos personalizados | **Desarrollo, test y CI** |
| `update` | Intenta modificar el esquema para que coincida | Cómodo en prototipos | Cambios impredecibles, pérdida de datos, columnas renombradas como nuevas, viola la fuente de verdad | **Nunca** |
| `create` | Elimina y crea el esquema al arrancar | Útil solo para prototipos desechables | Destruye datos; ignora el esquema real | **Nunca** |
| `create-drop` | Crea al arrancar y elimina al cerrar | Pruebas muy aisladas | Destruye datos; no prueba el esquema real | **Nunca** |

### 3.2 Recomendación por entorno

| Entorno | `ddl-auto` | Validación adicional |
|---------|------------|----------------------|
| `local` | `validate` | El desarrollador ve inmediatamente si el mapeo no coincide con `db/esquema.sql` |
| `test` | `validate` | Testcontainers con `db/esquema.sql`; el contexto falla si el mapeo no coincide |
| `ci` | `validate` | Job dedicado de contrato de esquema + pruebas de repositorio |
| `prod` | `none` | Validación **previa** en staging/CI con `validate`; usuario de aplicación sin DDL |

**Por qué `none` en producción:** el arranque no debe depender de que el usuario
de aplicación pueda leer metadatos ni de que el esquema esté perfectamente
alineado en cada despliegue. La validación se hace como *gate* de despliegue,
no como efecto secundario del arranque. Si operaciones prefiere `validate`
también en producción, es aceptable siempre que se midan arranque y permisos; es
una decisión pendiente.

**Por qué `validate` en desarrollo/test:** es la forma barata de detectar que una
entidad quedó desalineada tras un cambio de esquema. No sustituye las pruebas de
repositorio: `validate` no comprueba CHECK, FK, UNIQUE, índices ni defaults.

### 3.3 Propiedades propuestas

| Propiedad | Valor | Motivo |
|-----------|-------|--------|
| `spring.jpa.hibernate.ddl-auto` | `validate` (local/test/ci), `none` (prod) | Ver §3.2 |
| `spring.jpa.generate-ddl` | `false` | Refuerza que Hibernate no genere DDL |
| `spring.jpa.open-in-view` | `false` | Evita lazy loading fuera de la transacción y conexiones retenidas |
| `spring.jpa.properties.hibernate.jdbc.time_zone` | `UTC` | Coherencia con `timestamptz` |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | `50` | Lotes de inserción/actualización |
| `spring.jpa.properties.hibernate.order_inserts` | `true` | Agrupa inserts por entidad |
| `spring.jpa.properties.hibernate.order_updates` | `true` | Agrupa updates por entidad |
| `spring.jpa.properties.hibernate.default_batch_fetch_size` | `32` | Reduce N+1 en colecciones |
| `spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch` | `true` | Evita paginación en memoria |
| `spring.jpa.properties.hibernate.enable_lazy_load_no_trans` | `false` | Prohibido cargar fuera de transacción |
| `spring.jpa.properties.hibernate.format_sql` | `true` solo local | Legibilidad; no en prod |
| `spring.jpa.show-sql` | `true` solo local | Diagnóstico; no en prod |
| `logging.level.org.hibernate.SQL` | `debug` solo local | Diagnóstico; no en prod |
| `spring.jpa.properties.hibernate.dialect` | No se fija | Hibernate detecta PostgreSQL |
| `spring.jpa.hibernate.naming.physical-strategy` | Default, con nombres explícitos | Evita dependencia de convenciones implícitas |

Notas:

- No se fija `hibernate.dialect` ni se sobrescriben versiones administradas por
  el BOM.
- `spring.datasource.hikari.maximum-pool-size` se dimensiona según la base y el
  número de instancias; un valor inicial razonable es 10, pero no es una regla
  arquitectónica.
- `spring.sql.init.mode=never` en producción. En pruebas, el esquema se carga
  desde `db/esquema.sql` en el contenedor efímero, no en la base de desarrollo.

## 4. Convenciones de mapeo

### 4.1 Entidades JPA

- Clases en `<módulo>.adapter.out.persistence.entity`, sufijo `Entity`.
- `@Table(name = "...", schema = "public")` explícito en todas.
- `@Column(name = "...", nullable = ..., length = ..., precision = ..., scale = ...)`
  explícito en todas las columnas.
- No se usa Lombok ni `@Data`; igualdad/hashCode de entidad basada en ID.
- Constructor sin argumentos requerido por JPA (puede ser `protected`).
- Sin setters públicos; el adaptador usa mappers.
- Sin `@Entity` en el dominio.
- Sin asociaciones JPA entre módulos.
- Solo los hijos internos de un agregado tienen asociaciones JPA:
  `Instrumento → IdentificadorInstrumento`,
  `ContratoInversion → HistorialTasa/PagoRendimiento`.

### 4.2 Identificadores

- Tipo `java.util.UUID`.
- **Asignados por la aplicación/dominio** (`UUID.randomUUID()` en la fábrica o
  pasado explícitamente en pruebas). No se usa `@GeneratedValue`.
- El `DEFAULT gen_random_uuid()` de la base queda como red de seguridad para
  inserciones manuales, no como mecanismo principal.
- Razón: el agregado tiene identidad antes de persistir, las pruebas son
  deterministas y se evita un viaje extra a la base.

### 4.3 Fechas y horas

- `date` → `LocalDate`.
- `timestamptz` → `Instant` en UTC; `hibernate.jdbc.time_zone=UTC`.
- Las marcas de creación/actualización (`fecha_creacion`,
  `fecha_actualizacion`, `fecha_apertura`) se generan en la aplicación con
  `java.time.Clock` para que las pruebas sean deterministas. Los `DEFAULT now()`
  de la base quedan como respaldo.
- No se usa `LocalDateTime` para `timestamptz`.

### 4.4 Números

| Columna | Tipo PostgreSQL | Tipo Java | Escala |
|---------|-----------------|-----------|--------|
| Dinero (`importe`, `valor_mercado`, `valor_costo`, `ganancia_perdida`, `precio`, `precio_promedio`, `comision` según columna) | `numeric(24,8)` / `numeric(20,8)` | `BigDecimal` | Exacta según columna |
| Cantidades (`cantidad`) | `numeric(28,10)` | `BigDecimal` | 10 |
| Tasas (`tasa`, `tasa_contractual` si v2) | `numeric(12,8)` | `BigDecimal` | 8 |
| Tipos de cambio | `numeric(20,10)` | `BigDecimal` | 10 |
| `decimales` | `integer` | `int` | — |

- Nunca `double`/`float`.
- **Convención propuesta (pendiente de confirmar):** las tasas se guardan en
  fracción anual (`0.0950` = 9.5 %), no en porcentaje. El esquema no impone esta
  semántica; hay que acordarla con negocio antes de implementar.
- **Convención propuesta:** los cálculos usan `RoundingMode.HALF_UP` y una escala
  explícita; al persistir se respeta la escala de la columna. La política de
  redondeo debe aprobarse junto con las fórmulas de devengo.
- La comisión es un costo separado; no se suma silenciosamente al `importe`.

### 4.5 Booleanos y enums

- `boolean` → `boolean`.
- Enums con CHECK en la base → `@Enumerated(EnumType.STRING)`:
  - `membresia.rol`: `PROPIETARIO`, `ADMINISTRADOR`, `EDITOR`, `LECTOR`.
  - `contrato_inversion.modalidad_interes`: `SIMPLE`, `COMPUESTO`.
- Texto libre sin CHECK (`tipo_movimiento.categoria`,
  `pago_rendimiento.tipo`, `identificador_instrumento.tipo`,
  `periodicidad_calculo`, `periodicidad_pago`, `institucion.tipo`) → value
  object/String validado, **no** enum inventado. Los valores permitidos están
  pendientes de decisión.
- Un valor desconocido en un enum mapeado falla de forma explícita; no se
  convierte en `null`.

### 4.6 Bajas lógicas

- `activo=false` es la baja lógica; no se borran filas.
- Todas las FK son `ON DELETE RESTRICT`; un borrado físico falla si hay
  referencias.
- El adaptador no expone `delete` salvo para tareas administrativas aprobadas.

### 4.7 Concurrencia

- El esquema **no tiene columna `version`**; no se puede usar `@Version`.
- Se usa bloqueo pesimista (`PESSIMISTIC_WRITE`) en los agregados que reciben
  escrituras concurrentes.
- Si se quiere bloqueo optimista, se requiere un cambio de esquema aprobado
  (columna `version`), que no forma parte de esta tarea.

## 5. Mapeo tabla → entidad → dominio

| # | Tabla | Módulo | Entidad JPA | Agregado / objeto de dominio | Asociaciones JPA | Notas de mapeo |
|---|-------|--------|-------------|------------------------------|------------------|----------------|
| 1 | `usuario` | `acceso` | `UsuarioEntity` | `Usuario` (raíz) | Ninguna | `password_hash` → `PasswordHash`; `email` → `Email`; timestamps con `Clock` |
| 2 | `portafolio` | `acceso` | `PortafolioEntity` | `Portafolio` (raíz) | Ninguna | `moneda_base_id` → `MonedaId`; `descripcion` opcional |
| 3 | `membresia` | `acceso` | `MembresiaEntity` | `Membresia` (raíz) | Ninguna | `usuario_id`/`portafolio_id` UUID; `rol` enum; UNIQUE (usuario, portafolio) |
| 4 | `institucion` | `cuentas` | `InstitucionEntity` | `Institucion` (raíz) | Ninguna | `tipo`, `pais`, `sitio_web` opcionales |
| 5 | `cuenta_financiera` | `cuentas` | `CuentaFinancieraEntity` | `CuentaFinanciera` (raíz) | Ninguna | `portafolio_id`, `institucion_id`, `tipo_cuenta_id`, `moneda_id` UUID; UNIQUE parcial de número |
| 6 | `moneda` | `catalogos` | `MonedaEntity` | `Moneda` (raíz) | Ninguna | `codigo` → `CodigoMoneda`; CHECK de formato/uppercase |
| 7 | `clase_activo` | `catalogos` | `ClaseActivoEntity` | `ClaseActivo` (raíz) | Ninguna | `nombre` UNIQUE |
| 8 | `tipo_instrumento` | `catalogos` | `TipoInstrumentoEntity` | `TipoInstrumento` (raíz) | Ninguna | `clase_activo_id` UUID; UNIQUE (clase, nombre) |
| 9 | `tipo_cuenta` | `catalogos` | `TipoCuentaEntity` | `TipoCuenta` (raíz) | Ninguna | `nombre` UNIQUE |
| 10 | `tipo_movimiento` | `catalogos` | `TipoMovimientoEntity` | `TipoMovimiento` (raíz) | Ninguna | `afecta_posicion` boolean; `categoria` texto libre |
| 11 | `tipo_cambio` | `mercado` | `TipoCambioEntity` | `TipoCambio` (raíz) | Ninguna | `moneda_origen_id`/`destino_id` UUID; `tipo_cambio` > 0; UNIQUE (origen, destino, fecha) |
| 12 | `instrumento` | `instrumentos` | `InstrumentoEntity` | `Instrumento` (raíz) | `identificadores` 1:N (hijo) | `tipo_instrumento_id`, `moneda_id` UUID; UNIQUE parcial (símbolo, moneda) |
| 13 | `identificador_instrumento` | `instrumentos` | `IdentificadorInstrumentoEntity` | `IdentificadorInstrumento` (hijo) | `@ManyToOne` a `InstrumentoEntity` | UNIQUE (instrumento, tipo); `tipo` texto libre |
| 14 | `posicion` | `posiciones` | `PosicionEntity` | `Posicion` (raíz) | Ninguna | `cuenta_id`, `instrumento_id`, `moneda_id` UUID; **`contrato_id` se ignora** (legado) |
| 15 | `movimiento` | `posiciones` | `MovimientoEntity` | `Movimiento` (raíz) | Ninguna | `posicion_id` UUID (agregado separado); `tipo_movimiento_id`, `moneda_id` UUID |
| 16 | `contrato_inversion` | `contratos` | `ContratoInversionEntity` | `ContratoInversion` (raíz) | `historialTasas`/`pagos` 1:N (hijos) | `posicion_id` UUID; `tasa` NOT NULL; `modalidad_interes` enum |
| 17 | `historial_tasa` | `contratos` | `HistorialTasaEntity` | `HistorialTasa` (hijo) | `@ManyToOne` a `ContratoInversionEntity` | `fecha` NOT NULL; `fecha_inicio`/`fecha_fin` opcionales; UNIQUE (contrato, fecha) |
| 18 | `pago_rendimiento` | `contratos` | `PagoRendimientoEntity` | `PagoRendimiento` (hijo) | `@ManyToOne` a `ContratoInversionEntity` | `moneda_id` UUID; `tipo` texto libre; sin UNIQUE natural |
| 19 | `cotizacion` | `mercado` | `CotizacionEntity` | `Cotizacion` (raíz) | Ninguna | `instrumento_id`, `moneda_id` UUID; UNIQUE (instrumento, fecha, moneda) |
| 20 | `valoracion_historica` | `valoraciones` | `ValoracionHistoricaEntity` | `ValoracionHistorica` (raíz) | Ninguna | `posicion_id`, `moneda_id` UUID; importes NULL en DB; `ganancia_perdida` columna normal |

### 5.1 Columnas que **no** se mapean hoy

| Columna / tabla | Motivo | Estado |
|-----------------|--------|--------|
| `posicion.contrato_id` | Relación duplicada; la vía canónica es `contrato_inversion.posicion_id` | Se ignora; v2 N1 propone eliminarla |
| `plan_ppr` | No existe en el esquema actual | Solo en v2 opcional |
| `contrato_inversion.tipo_tasa`, `tasa_referencia`, `spread`, `base_calculo_dias`, `reinvierte_intereses`, `renovacion_automatica`, `contrato_origen_id` | No existen en el esquema actual | Solo en v2 N2 |
| `historial_tasa.vigencia`, `fecha_registro`, `motivo` | No existen en el esquema actual | Solo en v2 N3 |
| `valoracion_historica.metodo`, `estado`, `fuente`, `cotizacion_id` | No existen en el esquema actual | Solo en v2 N4 |
| `movimiento.importe_convertido`, `tipo_cambio_aplicado`, `fecha_tipo_cambio`, `fuente_tipo_cambio` | No existen en el esquema actual | Solo en v2 N5 |
| `posicion.lote`, `movimiento.movimiento_origen_id`, `tipo_movimiento.naturaleza` | No existen en el esquema actual | Solo en v2 opcional |

No se crean entidades “preparadas” para columnas que no existen: `validate` no
las validaría y el código quedaría engañoso. Se añadirán cuando el cambio de
esquema esté aprobado y aplicado.

## 6. Transformación dominio ↔ persistencia

### 6.1 Mappers

- Un mapper por agregado en `adapter.out.persistence.mapper`.
- Métodos:
  - `toDomain(Entity)` → agregado.
  - `toEntity(Domain)` → entidad nueva.
  - `updateEntity(Entity, Domain)` → actualiza una entidad existente.
- Mappers manuales; sin MapStruct, sin reflexión, sin conversión automática.
- `toDomain` valida invariantes; si los datos persistidos son incoherentes,
  lanza `DatosPersistidosInvalidos` (excepción de infraestructura/dominio según
  el caso) y no devuelve un agregado inválido.
- `toEntity` no genera IDs nuevos si el dominio ya los tiene.
- `updateEntity` no reemplaza la instancia JPA; copia campos para preservar
  identidad y estado de persistencia.

### 6.2 Valores NULL y opcionales

| Columna nullable | Representación en dominio | Regla |
|------------------|---------------------------|-------|
| `portafolio.descripcion` | `Optional<String>` | Ausente es válido |
| `institucion.tipo`, `pais`, `sitio_web` | `Optional<String>` | Ausente es válido |
| `cuenta_financiera.numero_cuenta` | `Optional<NumeroCuenta>` | UNIQUE parcial solo si existe |
| `cuenta_financiera.fecha_apertura`, `fecha_cierre` | `Optional<LocalDate>` | Si ambas, `cierre >= apertura` |
| `instrumento.simbolo`, `emisor`, `descripcion` | `Optional<...>` | UNIQUE parcial solo si símbolo existe |
| `identificador_instrumento.pais` | `Optional<String>` | Ausente es válido |
| `contrato_inversion.periodicidad_calculo`, `periodicidad_pago` | `Optional<Periodicidad>` | Valores permitidos pendientes |
| `contrato_inversion.fecha_vencimiento` | `Optional<LocalDate>` | Si existe, `>= fecha_inicio` |
| `historial_tasa.fecha_inicio`, `fecha_fin` | `Optional<LocalDate>` | Si ambas, `fin >= inicio` |
| `pago_rendimiento.tipo`, `descripcion` | `Optional<...>` | Texto libre |
| `cotizacion.fuente` | `Optional<String>` | Procedencia opcional |
| `tipo_cambio.fuente` | `Optional<String>` | Procedencia opcional |
| `valoracion_historica.valor_mercado`, `valor_costo` | **No opcional en dominio** | Si la DB trae NULL, se considera dato corrupto (decisión pendiente) |
| `valoracion_historica.ganancia_perdida` | Calculada | Se persiste y se valida contra `valor_mercado - valor_costo` |

### 6.3 Reglas de coherencia en el mapper

- `ganancia_perdida` debe ser `valor_mercado - valor_costo` (misma escala). Si
  la columna trae un valor distinto, se registra/falla; no se corrige en
  silencio.
- `moneda_id` de `valoracion_historica` debe coincidir con la moneda de la
  posición; si no, es un dato incoherente (la base no lo impone).
- `movimiento.moneda_id` puede diferir de `posicion.moneda_id`; el esquema
  actual no guarda la conversión. El dominio debe exponer ambos importes solo
  cuando exista la columna; hoy no se inventa.
- `contrato_inversion.posicion_id` debe existir; la FK lo garantiza.
- `historial_tasa` no debe solaparse; la base actual no lo impone. Se valida en
  el caso de uso y queda una ventana de carrera; v2 N3 la cierra.
- `pago_rendimiento` puede duplicarse; la idempotencia es una decisión de
  aplicación pendiente.

## 7. Repositorios y consultas

### 7.1 Estructura

- `XJpaRepository` (Spring Data) en `adapter.out.persistence.repository`.
- `XRepositoryAdapter` implementa el puerto de dominio `XRepository`.
- `XQueryAdapter` implementa los puertos de `<módulo>.api`.
- El adaptador usa el repositorio Spring Data y los mappers; no expone
  `JpaRepository` fuera del paquete.

### 7.2 Reglas de consulta

- Métodos explícitos (`buscarPorEmail`, `listarPorPortafolio`,
  `buscarParaActualizar`), no `findAll()` sin filtro.
- Paginación con `PageQuery`/`PageResult` de `shared.application`; el adaptador
  traduce a `Pageable`/`Page` internamente.
- Ordenamiento por lista blanca; no se acepta cualquier campo desde la API.
- Proyecciones para listados: no se carga el agregado completo si solo se
  necesitan columnas.
- `exists` para validaciones; no se usa `getReferenceById` como validación.
- JPQL por defecto. SQL nativo solo si PostgreSQL lo exige (por ejemplo, rangos
  de v2, que no están aplicados).
- Sin joins entre tablas de módulos distintos. Si se necesitan, se crea un read
  model o se amplía un puerto de consulta.
- Sin `JpaSpecificationExecutor` expuesto en la API del módulo.

### 7.3 Bloqueo pesimista

- `PosicionRepository.buscarParaActualizar(id)` usa `PESSIMISTIC_WRITE`.
- `ContratoInversionRepository.buscarParaActualizar(id)` para añadir tasas/pagos.
- El bloqueo se mantiene dentro de la transacción del caso de uso.
- Orden de bloqueo: `Portafolio` → `CuentaFinanciera` → `Posicion` →
  `ContratoInversion`.
- No se usa `LockModeType.PESSIMISTIC_READ` salvo para reportes que lo
  justifiquen.

## 8. Transacciones y concurrencia

| Aspecto | Decisión |
|---------|----------|
| Dónde se declara | Servicio de aplicación (`@Transactional`) |
| Propagación | `REQUIRED` por defecto |
| Aislamiento | `READ_COMMITTED` por defecto; `REPEATABLE_READ` solo para reportes que necesiten snapshot |
| `readOnly` | `true` en consultas |
| Open Session in View | `false` |
| Escrituras por transacción | Un solo módulo |
| Bloqueo optimista | No disponible (sin columna `version`) |
| Bloqueo pesimista | Sí, en agregados con escritura concurrente |
| Eventos | Opcionales, after-commit, no para consistencia fuerte |
| Lotes | Transacciones por chunk; idempotencia por clave única |

### 8.1 Casos especiales

- **`ValorarPortafolioUseCase`**: una transacción por posición, no una global.
  El resultado puede ser parcial; se reportan posiciones fallidas. Se implementa
  con un bean separado o `TransactionTemplate`, no con autollamada
  `@Transactional` (que no se aplica).
- **`ImportarCotizacionesUseCase` / `ImportarTiposCambioUseCase`**: lotes con
  `upsert` por clave única; si una fila falla, se registra y se continúa o se
  aborta el lote según la política que se apruebe.
- **`RegistrarMovimientoUseCase`**: bloquea la posición, actualiza su estado e
  inserta el movimiento en la misma transacción. Si falla el insert, no se
  actualiza la posición.
- **`RegistrarTasaUseCase`**: bloquea el contrato, valida no-solape en
  aplicación (mientras v2 no esté aplicada) e inserta. La carrera entre dos
  transacciones sigue siendo posible; se documenta como brecha.

## 9. Restricciones y traducción de errores

### 9.1 SQLSTATE

| SQLSTATE | Significado | Excepción de dominio/aplicación | HTTP |
|----------|-------------|---------------------------------|------|
| `23505` | `unique_violation` | `ConflictoUnicidad` | 409 |
| `23503` | `foreign_key_violation` | `ReferenciaInvalida` / `EntidadEnUso` | 409/422 |
| `23514` | `check_violation` | `DatoInvalido` | 422 |
| `23502` | `not_null_violation` | `DatoRequerido` | 422 |
| `23P01` | `exclusion_violation` | `ConflictoDeVigencia` | 409 |

`23P01` solo aparecería si se aplica la propuesta v2 N3-A; hoy no existe.

### 9.2 Restricciones relevantes

| Restricción | Tabla | Traducción |
|-------------|-------|------------|
| `uq_usuario_email` | `usuario` | Email duplicado |
| `uq_moneda_codigo` | `moneda` | Código de moneda duplicado |
| `uq_clase_activo_nombre` | `clase_activo` | Nombre duplicado |
| `uq_tipo_instrumento` | `tipo_instrumento` | Tipo duplicado dentro de la clase |
| `uq_tipo_cuenta_nombre` | `tipo_cuenta` | Nombre duplicado |
| `uq_tipo_movimiento_nombre` | `tipo_movimiento` | Nombre duplicado |
| `uq_membresia_usuario_portafolio` | `membresia` | Membresía duplicada |
| `uq_cuenta_financiera_numero` | `cuenta_financiera` | Número duplicado en la institución |
| `uq_instrumento_simbolo_moneda` | `instrumento` | Símbolo duplicado en la moneda |
| `uq_identificador_instrumento_tipo` | `identificador_instrumento` | Identificador duplicado para el instrumento |
| `uq_historial_tasa` | `historial_tasa` | Tasa duplicada para contrato/fecha |
| `uq_cotizacion` | `cotizacion` | Cotización duplicada para instrumento/fecha/moneda |
| `uq_valoracion_historica` | `valoracion_historica` | Valoración duplicada para posición/fecha |
| `uq_tipo_cambio` | `tipo_cambio` | Tipo de cambio duplicado para par/fecha |
| 28 FK | Todas | Referencia inexistente o entidad en uso (`RESTRICT`) |
| 18 CHECK | Todas | Dato inválido (formato, rango, fechas, enums) |

CHECKs del esquema actual: `ck_moneda_codigo_formato`, `ck_moneda_decimales`,
`ck_usuario_email`, `ck_membresia_rol`, `ck_cuenta_financiera_fechas`,
`ck_posicion_precio_promedio`, `ck_posicion_fechas`, `ck_contrato_tasa`,
`ck_contrato_modalidad`, `ck_contrato_fechas`, `ck_movimiento_precio`,
`ck_movimiento_comision`, `ck_historial_tasa_tasa`, `ck_historial_tasa_fechas`,
`ck_pago_rendimiento_importe`, `ck_cotizacion_precio`, `ck_tipo_cambio_par`,
`ck_tipo_cambio_valor`.

### 9.3 Dónde se traduce

- En el **adaptador de persistencia**: `DataIntegrityViolationException` →
  excepción de dominio/aplicación, usando SQLSTATE y, si está disponible, el
  nombre de la restricción.
- En el **adaptador REST**: excepción → `ProblemDetail` con `code` estable.
- Nunca en el controlador se inspecciona SQL ni nombres de tabla.

## 10. Validación de esquema y drift

### 10.1 Qué valida `validate` y qué no

| Valida | No valida |
|--------|-----------|
| Existencia de tablas mapeadas | Índices |
| Existencia de columnas mapeadas | CHECK |
| Tipos compatibles de columnas mapeadas | FK |
| Algunos tamaños/precisión | UNIQUE |
| — | Defaults |
| — | Columnas extra no mapeadas |
| — | Triggers, vistas, extensiones |

Por eso `validate` es un primer filtro, no una prueba de contrato completa.

### 10.2 Job de contrato de esquema

1. Levantar un PostgreSQL 16 efímero (Testcontainers).
2. Aplicar `db/esquema.sql` tal cual.
3. Arrancar el contexto con `ddl-auto=validate`.
4. Ejecutar pruebas de repositorio que cubran UNIQUE, FK, CHECK, precisión y
   fechas.
5. Comparar la huella del `pg_dump` con la esperada (como en
   `docs/schema-review.md`).

Si el contexto falla con `validate`, el mapeo y el esquema están desalineados;
no se “arregla” con `update`.

### 10.3 Drift en producción

- `ddl-auto=none` no detecta drift en arranque.
- El gate de despliegue ejecuta el job de §10.2 contra el esquema de
  staging/restaurado.
- Se registra la versión del esquema aplicado (huella del dump) en el pipeline.
- Si se detecta drift, se corrige el esquema o el mapeo; nunca se deja que
  Hibernate lo corrija.

## 11. Brechas del esquema actual y impacto de la v2

| Elemento | Esquema actual | Mapeo/dominio hoy | Propuesta v2 | Estado |
|----------|----------------|-------------------|--------------|--------|
| Relación posición–contrato | `posicion.contrato_id` + `contrato_inversion.posicion_id` | Se ignora `posicion.contrato_id`; se usa `contrato_inversion.posicion_id` | N1 elimina la columna duplicada | Pendiente de aprobación |
| Importes de valoración | `valor_mercado`, `valor_costo`, `ganancia_perdida` admiten NULL | Dominio exige importes; mapper falla si NULL | N4 los hace NOT NULL y deriva la ganancia | Pendiente |
| Historial de tasas | `fecha`, `fecha_inicio`, `fecha_fin` | Se mapean las tres columnas; no-solape en aplicación | N3 usa `daterange` + exclusión/trigger | Pendiente |
| Tasa contractual | `tasa` NOT NULL | `Tasa` no nula | N2 separa fija/variable y permite NULL en variable | Pendiente |
| Movimientos multidivisa | Sin columnas de conversión | Solo importe original + moneda | N5 añade importe convertido, tasa, fecha y fuente | Pendiente |
| Bloqueo optimista | Sin `version` | Bloqueo pesimista | No incluido | Pendiente si se desea |
| PPR | Sin `plan_ppr` | PPR como cuenta/instrumento | O4 opcional | Pendiente |
| Renovaciones | Sin `contrato_origen_id` | No se modelan | N2 añade autorreferencia validada | Pendiente |
| Reversas de movimientos | Sin `movimiento_origen_id` | No se modelan | O2 opcional | Pendiente |
| Lotes de posición | Sin `lote` | Una posición por cuenta/instrumento (sin UNIQUE) | O1 opcional | Pendiente |
| Naturaleza de movimiento | Sin `naturaleza` | Se deduce de `categoria`/signo (pendiente) | O3 opcional | Pendiente |
| Idempotencia de pagos | Sin UNIQUE natural | Puede duplicarse | No incluido | Pendiente de decisión |

**Regla:** ninguna entidad se adelanta a la v2. Si se aprueba, se actualiza el
mapeo, los mappers y las pruebas en una tarea separada.

## 12. Rendimiento e índices

| Consulta típica | Índice disponible | Nota |
|-----------------|-------------------|------|
| Posiciones por cuenta | `idx_posicion_cuenta` | — |
| Posiciones por instrumento | `idx_posicion_instrumento` | — |
| Movimientos por posición | `idx_movimiento_posicion` | — |
| Movimientos por fecha | `idx_movimiento_fecha` | — |
| Movimientos por posición y rango de fechas | No hay compuesto | `idx_movimiento_posicion` + filtro; evaluar compuesto en una propuesta futura |
| Contratos por posición | `idx_contrato_inversion_posicion` | — |
| Tasas por contrato y fecha | `idx_historial_tasa_contrato_fecha` | `(contrato_id, fecha DESC)` |
| Pagos por contrato | `idx_pago_rendimiento_contrato` | — |
| Cotizaciones por instrumento y fecha | `idx_cotizacion_instrumento_fecha` | `(instrumento_id, fecha DESC)` |
| Valoraciones por posición y fecha | `idx_valoracion_historica_posicion_fecha` | `(posicion_id, fecha DESC)` |
| Tipos de cambio por destino y fecha | `idx_tipo_cambio_destino` | UNIQUE `(origen, destino, fecha)` también indexa |
| Cuentas por portafolio | `idx_cuenta_financiera_portafolio` | — |
| Membresías por portafolio | `idx_membresia_portafolio` | — |
| Instrumentos por tipo/moneda | `idx_instrumento_tipo`, `idx_instrumento_moneda` | — |
| Identificadores por tipo/valor | `idx_identificador_instrumento_valor` | — |
| Usuarios activos | `idx_usuario_activo` | — |
| Instituciones por nombre | `idx_institucion_nombre` | — |

Recomendaciones:

- Evitar N+1 con `@EntityGraph`, join fetch o `default_batch_fetch_size`.
- Usar proyecciones en listados.
- Paginar siempre; no cargar historiales completos.
- Revisar `EXPLAIN (ANALYZE, BUFFERS)` de las consultas lentas antes de añadir
  índices. Un índice nuevo es un cambio de esquema y requiere propuesta.
- No se añade caché en la primera iteración; PostgreSQL y el pool son
  suficientes para el volumen esperado.

## 13. Seguridad y permisos de base de datos

- El usuario de la aplicación debe tener permisos **DML** (`SELECT`, `INSERT`,
  `UPDATE`, `DELETE`) sobre las tablas necesarias, pero **no DDL**
  (`CREATE`, `ALTER`, `DROP`).
- Las migraciones/cambios los ejecuta un usuario administrador distinto, en un
  proceso controlado.
- Credenciales por variables de entorno; `.env` no se versiona.
- No se registran contraseñas, hashes ni datos personales en logs.
- El esquema no define roles de base de datos; la separación de privilegios es
  una decisión operativa pendiente.

## 14. Pruebas de persistencia

Resumen; el detalle está en `testing-strategy.md`.

- Testcontainers PostgreSQL 16 efímero; nunca la base `patrimonio` de
  desarrollo.
- Esquema desde `db/esquema.sql`; no `create-drop`.
- `ddl-auto=validate` en el contexto de prueba.
- Pruebas de mapeo, precisión, enums, fechas UTC, UNIQUE, FK `RESTRICT`, CHECK y
  bloqueo pesimista.
- `@DataJpaTest` con `@AutoConfigureTestDatabase(replace = NONE)` y el adaptador
  importado.
- Limpieza por rollback o truncado controlado; nunca en la base de desarrollo.

## 15. Decisiones pendientes

| # | Decisión | Impacto |
|---|----------|---------|
| 1 | `ddl-auto` en producción: `none` + gate, o `validate` | Arranque y permisos |
| 2 | Dominio estricto ante `valoracion_historica` NULL | Mapeo y manejo de datos heredados |
| 3 | Ignorar `posicion.contrato_id` hasta que se aplique v2 N1 | Mapeo actual y deuda técnica |
| 4 | Bloqueo pesimista vs. añadir columna `version` | Concurrencia y cambio de esquema |
| 5 | Validación de no-solape de tasas en aplicación mientras v2 no se aplica | Ventana de carrera |
| 6 | Idempotencia de `pago_rendimiento` | UNIQUE o control de aplicación |
| 7 | Mapeo de `ganancia_perdida` como columna normal o calculada | Coherencia y v2 N4 |
| 8 | Uso de SQL nativo para futuros rangos (`daterange`) | Portabilidad y pruebas |
| 9 | Tamaño del pool y timeouts | Operación |
| 10 | Permisos exactos del usuario de aplicación | Seguridad |
