# Patrimonio — Arquitectura hexagonal

> Documento de diseño. No contiene código Java/Kotlin ejecutable ni modifica la
> base de datos. Fecha: 2026-10-23.
>
> Complementa `architecture-overview.md` y `module-boundaries.md`.

## 1. Principios

1. **El dominio no sabe dónde vive.** No importa Spring, JPA, Hibernate,
   Jackson, Jakarta Validation ni PostgreSQL. Solo Java y su propio lenguaje.
2. **Las dependencias apuntan hacia dentro.** Adaptadores → aplicación →
   dominio. Nunca al revés.
3. **Los puertos son del módulo, no del framework.** Un puerto es una interfaz
   con lenguaje de negocio; no es un `Repository<T, ID>` genérico.
4. **Un caso de uso, un límite transaccional.** La transacción se abre y se
   cierra en el servicio de aplicación, no en el controlador ni en el
   repositorio.
5. **La persistencia es un detalle.** Las entidades JPA son un formato de
   almacenamiento; el modelo de dominio se construye a partir de ellas.
6. **La API es un contrato.** Los DTOs REST son estables y explícitos; las
   entidades JPA nunca se serializan.
7. **Proporcionalidad.** Sin capas ceremoniales, sin `BaseEntity`, sin
   `GenericService`, sin eventos si no hay un efecto secundario real.

## 2. Capas y responsabilidades

| Capa | Paquete | Responsabilidad | Puede depender de |
|------|---------|-----------------|-------------------|
| Dominio | `<módulo>.domain` | Agregados, entidades, value objects, servicios de dominio, invariantes, excepciones de negocio | `shared.domain` y su propio dominio |
| Aplicación | `<módulo>.application` | Casos de uso, orquestación, puertos, comandos/consultas, límites transaccionales | Su dominio, sus puertos, `shared.application` |
| Adaptador de entrada | `<módulo>.adapter.in.web` | HTTP, validación de forma, DTOs, mapeo a comandos/consultas, `ProblemDetail` | Su aplicación (`port.in`) |
| Adaptador de salida | `<módulo>.adapter.out.persistence` | JPA, entidades, mappers, repositorios Spring Data, consultas | Su aplicación (`port.out`), su dominio |
| API pública | `<módulo>.api` | Puertos de consulta y DTOs de lectura para otros módulos | Su dominio/aplicación, `shared` |
| Compartido | `shared` | IDs tipados, value objects comunes, paginación, excepciones base | Nada específico de un módulo |

### 2.1 Lo que **no** puede estar en el dominio

- Anotaciones `@Entity`, `@Table`, `@Column`, `@Id`, `@GeneratedValue`.
- `@Service`, `@Component`, `@Repository`, `@Transactional`.
- `JpaRepository`, `EntityManager`, `Pageable`, `Page`.
- `jakarta.validation.constraints.*` en campos del dominio. La validación de
  forma vive en los DTOs; las invariantes, en el dominio.
- Jackson (`@JsonProperty`, `@JsonIgnore`) u otras anotaciones de serialización.
- `Instant.now()`, `LocalDate.now()` o `UUID.randomUUID()` dispersos. El tiempo
  se inyecta (`java.time.Clock`) y los IDs se pasan o se generan en fábricas
  controladas.
- `double`/`float` para dinero, cantidades o tasas. Siempre `BigDecimal`.

## 3. Estructura de paquetes

```text
com.patrimonio
├── shared
│   ├── domain
│   │   ├── id            # UsuarioId, PortafolioId, CuentaId, ...
│   │   ├── money         # Importe, Cantidad, Tasa, TasaCambio, Comision
│   │   └── error         # DomainException, NotFoundException, ConflictException
│   └── application
│       ├── page          # PageQuery, PageResult
│       └── view          # vistas compartidas si hicieran falta
├── acceso
│   ├── api
│   │   ├── port          # PortafolioQueryPort, AccesoQueryPort
│   │   └── dto           # PortafolioResumen, PermisoPortafolio
│   ├── domain
│   │   ├── model         # Usuario, Portafolio, Membresia, RolMembresia
│   │   └── error
│   ├── application
│   │   ├── port.in       # CrearPortafolioUseCase, ...
│   │   ├── port.out      # UsuarioRepository, PortafolioRepository, ...
│   │   ├── command       # CrearPortafolioCommand, ...
│   │   ├── query         # ListarPortafoliosQuery, ...
│   │   ├── view          # PortafolioView, ...
│   │   └── service       # CrearPortafolioService, ...
│   └── adapter
│       ├── in.web        # PortafolioController, DTOs, assembler
│       └── out.persistence
│           ├── entity    # PortafolioEntity, ...
│           ├── repository# PortafolioJpaRepository
│           ├── mapper    # PortafolioMapper
│           └── adapter   # PortafolioRepositoryAdapter, PortafolioQueryAdapter
├── catalogos
├── cuentas
├── instrumentos
├── posiciones
├── contratos
├── mercado
├── valoraciones
└── PatrimonioApplication
```

Regla de visibilidad:

- Otros módulos solo pueden importar `com.patrimonio.<módulo>.api.*` y
  `com.patrimonio.shared.*`.
- `domain`, `application` y `adapter` son internos del módulo.
- No existe un paquete `com.patrimonio.service` o `com.patrimonio.repository`
  global que mezcle módulos.

## 4. Puertos de entrada

Un puerto de entrada representa **un caso de uso**, no un CRUD genérico. Se
nombra con verbo + sustantivo + `UseCase`. Vive en
`<módulo>.application.port.in` y no tiene anotaciones de Spring.

| Elemento | Convención |
|----------|------------|
| Nombre | `CrearPortafolioUseCase`, `RegistrarMovimientoUseCase`, `ValorarPosicionUseCase` |
| Método | `ejecutar(Command)` para escrituras; `consultar(Query)` para lecturas |
| Entrada | `record` inmutable en `application.command` o `application.query` |
| Salida | Vista de aplicación (`application.view`) o un `Resultado` explícito; nunca una entidad JPA |
| Errores | Excepciones de dominio/aplicación; no `ResponseEntity` ni códigos HTTP |
| Transacción | La declara el servicio que implementa el puerto, no la interfaz |

### 4.1 Comandos y consultas

- **Comando**: intención de cambiar estado (`CrearCuentaCommand`,
  `RegistrarMovimientoCommand`). Incluye IDs y datos validados de forma.
- **Consulta**: intención de leer (`ConsultarPosicionQuery`,
  `ListarMovimientosQuery`). Incluye filtros y paginación (`PageQuery`).
- Los comandos/consultas no contienen entidades de dominio ni objetos JPA.
- La validación de forma (campos obligatorios, longitudes, rangos simples) se
  hace en el DTO REST con Bean Validation; el caso de uso vuelve a validar las
  reglas de negocio que dependen del estado.

### 4.2 Catálogo de casos de uso por módulo

#### `acceso`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `CrearUsuarioUseCase` | Comando | Alta de usuario |
| `ActualizarUsuarioUseCase` | Comando | Datos de contacto/estado |
| `DesactivarUsuarioUseCase` | Comando | Baja lógica (`activo=false`) |
| `ConsultarUsuarioUseCase` | Consulta | Datos de un usuario |
| `CrearPortafolioUseCase` | Comando | Alta de portafolio con moneda base |
| `ActualizarPortafolioUseCase` | Comando | Nombre, descripción, moneda base |
| `ListarPortafoliosUseCase` | Consulta | Portafolios visibles para un usuario |
| `AsignarMembresiaUseCase` | Comando | Alta de membresía con rol |
| `CambiarRolMembresiaUseCase` | Comando | Cambio de rol |
| `RevocarMembresiaUseCase` | Comando | Baja de membresía |

#### `catalogos`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `ListarMonedasUseCase` / `CrearMonedaUseCase` / `ActualizarMonedaUseCase` / `DesactivarMonedaUseCase` | Consulta/comando | Gestión de monedas |
| `ListarClasesActivoUseCase` / `CrearClaseActivoUseCase` / `ActualizarClaseActivoUseCase` | Consulta/comando | Clases de activo |
| `ListarTiposInstrumentoUseCase` / `CrearTipoInstrumentoUseCase` / `ActualizarTipoInstrumentoUseCase` | Consulta/comando | Tipos de instrumento |
| `ListarTiposCuentaUseCase` / `CrearTipoCuentaUseCase` / `ActualizarTipoCuentaUseCase` | Consulta/comando | Tipos de cuenta |
| `ListarTiposMovimientoUseCase` / `CrearTipoMovimientoUseCase` / `ActualizarTipoMovimientoUseCase` | Consulta/comando | Tipos de movimiento |

Los catálogos son datos de referencia: se crean/actualizan con poca frecuencia y
se leen mucho. No se borran físicamente; se desactivan con `activo=false`.

#### `cuentas`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `CrearInstitucionUseCase` | Comando | Alta de institución |
| `ActualizarInstitucionUseCase` | Comando | Datos de institución |
| `AbrirCuentaUseCase` | Comando | Alta de cuenta financiera |
| `CerrarCuentaUseCase` | Comando | Cierre lógico/fecha de cierre |
| `ConsultarCuentaUseCase` | Consulta | Detalle de cuenta |
| `ListarCuentasPorPortafolioUseCase` | Consulta | Cuentas de un portafolio |

#### `instrumentos`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `CrearInstrumentoUseCase` | Comando | Alta de instrumento |
| `ActualizarInstrumentoUseCase` | Comando | Datos del instrumento |
| `AgregarIdentificadorUseCase` | Comando | Alta de identificador (ISIN, ticker, etc.) |
| `BuscarInstrumentoUseCase` | Consulta | Búsqueda por símbolo/identificador |

#### `posiciones`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `AbrirPosicionUseCase` | Comando | Alta de posición en una cuenta |
| `RegistrarMovimientoUseCase` | Comando | Registro de movimiento y actualización de la posición |
| `ConsultarPosicionUseCase` | Consulta | Detalle de posición |
| `ListarMovimientosUseCase` | Consulta | Historial de movimientos |
| `CerrarPosicionUseCase` | Comando | Baja lógica de la posición |

#### `contratos`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `CrearContratoUseCase` | Comando | Alta de contrato sobre una posición |
| `ActualizarContratoUseCase` | Comando | Datos contractuales |
| `RegistrarTasaUseCase` | Comando | Alta de tasa en el historial |
| `RegistrarPagoRendimientoUseCase` | Comando | Registro de pago |
| `ConsultarContratoUseCase` | Consulta | Detalle de contrato |
| `CerrarContratoUseCase` | Comando | Vencimiento/baja lógica |

#### `mercado`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `RegistrarCotizacionUseCase` | Comando | Alta de cotización |
| `ImportarCotizacionesUseCase` | Comando | Carga por lote |
| `RegistrarTipoCambioUseCase` | Comando | Alta de tipo de cambio |
| `ImportarTiposCambioUseCase` | Comando | Carga por lote |
| `ConvertirMonedaUseCase` | Consulta | Conversión con tasa de una fecha |

#### `valoraciones`

| Puerto de entrada | Tipo | Responsabilidad |
|-------------------|------|-----------------|
| `ValorarPosicionUseCase` | Comando | Calcula y guarda la valoración de una posición/fecha |
| `ValorarPortafolioUseCase` | Comando | Orquesta la valoración de varias posiciones |
| `ConsultarValoracionUseCase` | Consulta | Valoración de una posición/fecha |
| `ListarValoracionesUseCase` | Consulta | Serie histórica de valoraciones |

## 5. Puertos de salida

Un puerto de salida expresa una **necesidad del caso de uso**, no una capacidad
genérica de base de datos. Vive en `<módulo>.application.port.out`.

### 5.1 Repositorios de agregados

| Módulo | Puerto | Agregado |
|--------|--------|----------|
| `acceso` | `UsuarioRepository` | `Usuario` |
| `acceso` | `PortafolioRepository` | `Portafolio` |
| `acceso` | `MembresiaRepository` | `Membresia` |
| `catalogos` | `MonedaRepository` | `Moneda` |
| `catalogos` | `ClaseActivoRepository` | `ClaseActivo` |
| `catalogos` | `TipoInstrumentoRepository` | `TipoInstrumento` |
| `catalogos` | `TipoCuentaRepository` | `TipoCuenta` |
| `catalogos` | `TipoMovimientoRepository` | `TipoMovimiento` |
| `cuentas` | `InstitucionRepository` | `Institucion` |
| `cuentas` | `CuentaFinancieraRepository` | `CuentaFinanciera` |
| `instrumentos` | `InstrumentoRepository` | `Instrumento` |
| `posiciones` | `PosicionRepository` | `Posicion` |
| `posiciones` | `MovimientoRepository` | `Movimiento` |
| `contratos` | `ContratoInversionRepository` | `ContratoInversion` |
| `mercado` | `CotizacionRepository` | `Cotizacion` |
| `mercado` | `TipoCambioRepository` | `TipoCambio` |
| `valoraciones` | `ValoracionHistoricaRepository` | `ValoracionHistorica` |

Métodos típicos (nombres orientativos, no código):

- `buscarPorId`, `guardar`, `existe`, `listarPor...`, `buscarParaActualizar`.
- `buscarParaActualizar` usa bloqueo pesimista cuando el caso de uso modifica un
  agregado que puede recibir escrituras concurrentes.
- No se exponen métodos CRUD genéricos ni `findAll()` sin filtro/paginación.

### 5.2 Puertos de consulta entre módulos

Los publica el módulo proveedor en `<módulo>.api.port` y los implementa un
adaptador del proveedor. Son **de solo lectura** y devuelven DTOs inmutables de
`<módulo>.api.dto`, nunca agregados ni entidades.

| Módulo proveedor | Puerto | Consumidores | Devuelve |
|------------------|--------|--------------|----------|
| `catalogos` | `MonedaQueryPort` | `acceso`, `cuentas`, `instrumentos`, `posiciones`, `contratos`, `mercado`, `valoraciones` | `MonedaResumen` |
| `catalogos` | `TipoCuentaQueryPort` | `cuentas` | `TipoCuentaResumen` |
| `catalogos` | `TipoMovimientoQueryPort` | `posiciones` | `TipoMovimientoResumen` |
| `catalogos` | `TipoInstrumentoQueryPort` | `instrumentos` | `TipoInstrumentoResumen` |
| `catalogos` | `ClaseActivoQueryPort` | `instrumentos` | `ClaseActivoResumen` |
| `acceso` | `PortafolioQueryPort` | `cuentas` | `PortafolioResumen` |
| `acceso` | `AccesoQueryPort` | futuros módulos | `PermisoPortafolio` |
| `cuentas` | `CuentaQueryPort` | `posiciones` | `CuentaResumen` |
| `cuentas` | `InstitucionQueryPort` | `cuentas`/reportes | `InstitucionResumen` |
| `instrumentos` | `InstrumentoQueryPort` | `posiciones`, `mercado` | `InstrumentoResumen` |
| `posiciones` | `PosicionQueryPort` | `contratos`, `valoraciones` | `PosicionResumen` |
| `mercado` | `CotizacionQueryPort` | `valoraciones` | `CotizacionResumen` |
| `mercado` | `TipoCambioQueryPort` | `posiciones` (multidivisa), `valoraciones` | `TipoCambioResumen` |

Reglas:

- Un puerto de consulta no escribe ni abre transacciones propias; se une a la
  transacción del caso de uso que lo invoca.
- No se permite encadenar consultas de varios módulos dentro de un bucle sin
  control; si aparece un N+1 entre módulos, se diseña una consulta específica o
  un read model, no se accede a tablas ajenas.
- Los DTOs de `api` no tienen anotaciones JPA ni Jackson; son datos planos.

### 5.3 Otros puertos

| Puerto | Módulo | Uso |
|--------|--------|-----|
| `PasswordHasher` | `acceso` | Hash/verificación de contraseña; **pendiente** de decidir si se usa Spring Security u otra librería |
| `CotizacionQueryPort` | `mercado` | Última cotización disponible para instrumento/fecha/moneda |
| `TipoCambioQueryPort` | `mercado` | Tasa para par/fecha y conversión de importes |

No se crea un `ClockPort`: se inyecta `java.time.Clock` (JDK) en los servicios de
aplicación y se pasa la fecha/hora explícita al dominio. En pruebas se usa
`Clock.fixed(...)`.

## 6. Servicios de aplicación y límites transaccionales

Un servicio de aplicación:

1. Recibe un comando/consulta.
2. Carga los agregados necesarios a través de puertos.
3. Invoca lógica de dominio (métodos del agregado o servicios de dominio).
4. Persiste a través de puertos.
5. Devuelve una vista de aplicación.
6. Declara la transacción.

| Caso de uso | Transacción | Bloqueos | Escrituras |
|-------------|-------------|----------|------------|
| `RegistrarMovimientoUseCase` | `READ_WRITE` | `Posicion` con `PESSIMISTIC_WRITE` | `posicion`, `movimiento` |
| `AbrirPosicionUseCase` | `READ_WRITE` | Ninguno | `posicion` |
| `CrearContratoUseCase` | `READ_WRITE` | Ninguno | `contrato_inversion` |
| `RegistrarTasaUseCase` | `READ_WRITE` | `ContratoInversion` con `PESSIMISTIC_WRITE` | `historial_tasa` |
| `RegistrarPagoRendimientoUseCase` | `READ_WRITE` | `ContratoInversion` con `PESSIMISTIC_WRITE` | `pago_rendimiento` |
| `ValorarPosicionUseCase` | `READ_WRITE` | Ninguno (upsert por clave única) | `valoracion_historica` |
| `ValorarPortafolioUseCase` | Orquestación | Ninguno | Una transacción por posición |
| `ImportarCotizacionesUseCase` | Lotes | Ninguno | `cotizacion` por lotes |
| Consultas | `READ_ONLY` | Ninguno | Ninguna |

Notas:

- `@Transactional` se declara en el método público del servicio de aplicación.
  Es la única dependencia de Spring permitida en esa capa; el dominio sigue
  puro.
- Los controladores **no** llevan `@Transactional`.
- Los adaptadores de persistencia **no** abren transacciones; se unen a la del
  caso de uso.
- `ValorarPortafolioUseCase` no debe ejecutar todo en una sola transacción: una
  posición lenta o inválida no debe bloquear el resto. Se documenta como
  orquestación con transacciones independientes y resultado parcial.
- No hay transacciones distribuidas ni `XA`: un caso de uso escribe en un solo
  módulo. Si un flujo futuro requiere escrituras en dos módulos, se diseña un
  caso de uso orquestador y un mecanismo de compensación/evento; no se asume
  ahora.

## 7. Adaptadores

### 7.1 Adaptador de entrada REST

- Un controlador por recurso o grupo coherente, en
  `<módulo>.adapter.in.web`.
- Recibe DTOs `record` con Bean Validation (`@Valid`).
- Convierte DTO → comando/consulta; no pasa el DTO a la aplicación.
- Invoca el puerto de entrada; no conoce repositorios ni entidades.
- Convierte la vista de aplicación → DTO de respuesta con un assembler.
- Devuelve `ResponseEntity`/`ProblemDetail`; los códigos HTTP viven aquí.
- No usa `Pageable` de Spring Data en la firma del caso de uso; construye
  `PageQuery` a partir de `page`/`size`.

### 7.2 Adaptador de salida JPA

- Entidades JPA anémicas en `<módulo>.adapter.out.persistence.entity`.
- Repositorios Spring Data en `...repository`, con métodos explícitos.
- Adaptador que implementa el puerto de dominio y usa el repositorio Spring
  Data + mappers.
- Mappers manuales en `...mapper`; no MapStruct (no está en el stack) ni
  reflexión.
- Consultas JPQL cuando sea posible; SQL nativo solo para capacidades
  PostgreSQL específicas (por ejemplo, rangos de la propuesta v2, que **no**
  está aplicada).
- No se expone `JpaRepository` fuera del adaptador.

### 7.3 Otros adaptadores

- **Importadores** (cotizaciones/tipos de cambio): adaptador de entrada
  programado o CLI, si se aprueba. No se crea scheduler en la primera iteración
  sin requisito.
- **Integraciones externas** (proveedor de precios, banco): adaptador de salida
  detrás de un puerto; no se implementa ahora.
- **Mensajería**: no hay broker; no se crea adaptador.

## 8. Modelo de dominio por módulo

| Módulo | Agregados (raíz) | Entidades internas | Value objects propios |
|--------|------------------|--------------------|-----------------------|
| `acceso` | `Usuario`, `Portafolio`, `Membresia` | — | `Email`, `PasswordHash`, `RolMembresia` |
| `catalogos` | `Moneda`, `ClaseActivo`, `TipoInstrumento`, `TipoCuenta`, `TipoMovimiento` | — | `CodigoMoneda`, `NombreCatalogo` |
| `cuentas` | `Institucion`, `CuentaFinanciera` | — | `NumeroCuenta`, `RangoFechas` |
| `instrumentos` | `Instrumento` | `IdentificadorInstrumento` | `Simbolo`, `TipoIdentificador` |
| `posiciones` | `Posicion`, `Movimiento` | — | `Cantidad`, `PrecioPromedio`, `Comision` |
| `contratos` | `ContratoInversion` | `HistorialTasa`, `PagoRendimiento` | `Tasa`, `ModalidadInteres`, `Periodicidad` |
| `mercado` | `Cotizacion`, `TipoCambio` | — | `Precio`, `TasaCambio`, `ParMonedas` |
| `valoraciones` | `ValoracionHistorica` | — | `ValorMercado`, `ValorCosto`, `GananciaPerdida` |

### 8.1 Reglas de modelado

- **Agregado = límite de consistencia.** Se guarda completo en una transacción.
- **Referencias entre agregados por ID**, no por objeto. `Movimiento` referencia
  `PosicionId`; `ContratoInversion` referencia `PosicionId`; `ValoracionHistorica`
  referencia `PosicionId`.
- **Entidades internas** solo cuando no tienen vida propia fuera del agregado:
  `IdentificadorInstrumento` dentro de `Instrumento`; `HistorialTasa` y
  `PagoRendimiento` dentro de `ContratoInversion`.
- **Value objects** para importes, cantidades, tasas, fechas y códigos; validan
  en su construcción.
- **Fábricas** (`crear`, `reconstruir`) para garantizar invariantes. `reconstruir`
  se usa al mapear desde persistencia y también valida; si los datos almacenados
  son incoherentes, falla de forma explícita.
- **Sin setters públicos** en el dominio; el estado cambia con métodos con
  intención (`registrarMovimiento`, `cambiarRol`, `cerrar`).
- **Sin herencia de entidades** salvo que exista una razón fuerte; no hay
  `BaseEntity`.

### 8.2 Decisiones de dominio pendientes

No se inventan reglas que el esquema no define:

| Tema | Estado | Qué falta decidir |
|------|--------|-------------------|
| Signo de `movimiento.cantidad` e `importe` | El esquema no impone CHECK | Reglas por tipo de movimiento (compra/venta/dividendo/etc.) |
| Actualización de `posicion.cantidad` y `precio_promedio` | El esquema solo guarda el estado | Método de coste (promedio/FIFO) y fórmula exacta |
| `tipo_movimiento.categoria` | Texto libre | Catálogo de categorías y su efecto |
| `periodicidad_calculo` / `periodicidad_pago` | Texto libre sin CHECK | Valores permitidos y semántica |
| `pago_rendimiento.tipo` | Texto libre | Catálogo o valores permitidos |
| `modalidad_interes` | CHECK `SIMPLE`/`COMPUESTO` | Fórmula de devengo y redondeo |
| `historial_tasa` sin solape | No hay restricción en el esquema actual | Aprobar v2 N3 o validar en aplicación |
| Valoración sin importes | Columnas NULL permitidas | ¿Dominio exige importes o modela `Optional`? |
| Multidivisa | No hay columnas de conversión | Aprobar v2 N5 o definir otra representación |

## 9. Transformación persistencia → dominio

Resumen; el detalle completo está en `persistence-strategy.md`.

### 9.1 Flujo

```text
Tabla PostgreSQL
   ↓ (JDBC/Hibernate)
Entidad JPA (anémica, mutable, solo datos)
   ↓ Mapper (toDomain)
Agregado / value objects (Java puro, invariantes)
   ↓ Caso de uso
Vista de aplicación
   ↓ Assembler
DTO REST
```

### 9.2 Reglas del mapper

- Un mapper por agregado, en el adaptador de persistencia.
- `toDomain(entity)`: reconstruye el agregado y valida invariantes.
- `toEntity(domain)`: crea una entidad nueva.
- `updateEntity(entity, domain)`: actualiza una entidad existente sin perder
  identidad JPA.
- No hay mappers bidireccionales automáticos ni reflexión.
- Los valores NULL se traducen explícitamente a `Optional` o a un error de datos,
  según la columna y la decisión de dominio.
- Los enums se mapean con `EnumType.STRING`; un valor desconocido en la base
  falla de forma explícita (no se convierte silenciosamente en `null`).
- Los `BigDecimal` conservan precisión y escala; no se convierten a `double`.
- Las fechas `date` se mapean a `LocalDate`; los `timestamptz` a `Instant` en UTC.

### 9.3 Prohibiciones

- Pasar una entidad JPA a un caso de uso o a un controlador.
- Anotar el agregado con `@Entity`.
- Usar el agregado como DTO de respuesta.
- Cargar relaciones perezosas fuera de la transacción (Open Session in View).
- Hacer `equals`/`hashCode` de entidades JPA basados en todos los campos.

## 10. Adaptador REST → aplicación

### 10.1 Contrato

| Paso | Responsable | Regla |
|------|-------------|-------|
| Deserializar JSON | Spring MVC | DTO `record` con tipos explícitos |
| Validar forma | Bean Validation | `@Valid` en el DTO; errores → 400 |
| Mapear a comando | Assembler del adaptador | Sin lógica de negocio |
| Ejecutar | Puerto de entrada | Una llamada por caso de uso |
| Mapear salida | Assembler | Vista → DTO de respuesta |
| Errores | `@RestControllerAdvice` | `ProblemDetail` RFC 9457 |

### 10.2 Reglas

- Un controlador no inyecta repositorios, `EntityManager` ni mappers de
  persistencia.
- Un controlador no devuelve `Page<Entidad>` ni una entidad JPA.
- Un controlador no abre transacciones.
- Los códigos HTTP se deciden en el adaptador, no en el dominio.
- La API no expone nombres de columnas, tablas ni SQL.
- Los listados usan paginación explícita (`page`, `size`) y un tamaño máximo.
- No se acepta ordenamiento arbitrario desde la API; se usa una lista blanca de
  campos.

### 10.3 Errores HTTP propuestos

| Situación | HTTP | `code` de ejemplo |
|-----------|------|-------------------|
| DTO inválido | 400 | `VALIDACION_ENTRADA` |
| Recurso no encontrado | 404 | `NO_ENCONTRADO` |
| Conflicto de unicidad | 409 | `DUPLICADO` |
| Referencia inexistente o en uso | 409/422 | `REFERENCIA_INVALIDA` |
| Regla de negocio incumplida | 422 | `REGLA_NEGOCIO` |
| Error inesperado | 500 | `ERROR_INTERNO` |

## 11. Transacciones y consistencia

### 11.1 Reglas

1. La transacción se abre en el servicio de aplicación.
2. Un caso de uso = una transacción, salvo orquestaciones por lote/posición.
3. Aislamiento por defecto: `READ_COMMITTED`.
4. No se usa `SERIALIZABLE` sin justificación y prueba de contención.
5. No hay `Open Session in View`; el mapeo ocurre dentro de la transacción.
6. Los eventos de dominio, si se usan, se publican **después del commit** para
   efectos secundarios no críticos. No se usan para consistencia fuerte.
7. No hay transacciones que abarquen dos módulos. Las lecturas entre módulos
   pueden participar en la misma transacción; las escrituras, no.

### 11.2 Concurrencia

El esquema actual **no tiene columna `version`**, por lo que no se puede usar
bloqueo optimista con `@Version`. Opciones:

- **Bloqueo pesimista** (`SELECT ... FOR UPDATE`) en los agregados que reciben
  escrituras concurrentes: `Posicion` al registrar movimientos; `ContratoInversion`
  al añadir tasas/pagos.
- **Restricciones únicas** para evitar duplicados por carrera.
- Si se necesita bloqueo optimista, se requiere un cambio de esquema (columna
  `version`), que no forma parte de esta tarea.

Orden de bloqueo sugerido para evitar deadlocks:
`Portafolio` → `CuentaFinanciera` → `Posicion` → `ContratoInversion`.

## 12. Pruebas como costura arquitectónica

Los puertos permiten probar sin infraestructura:

- Dominio: JUnit Jupiter 6.0.3 (BOM) + AssertJ, sin Spring.
- Casos de uso: puertos simulados (fakes o Mockito), sin contexto Spring.
- Adaptador REST: `@WebMvcTest` + puerto de entrada mockeado.
- Persistencia: `@DataJpaTest` + Testcontainers PostgreSQL 16.
- Integración: `@SpringBootTest` + Testcontainers.

El detalle está en `testing-strategy.md`.

## 13. Anti-patrones prohibidos

| Anti-patrón | Por qué se prohíbe |
|-------------|--------------------|
| `@Entity` en el dominio | Acopla el negocio a JPA y a la base |
| Controlador → repositorio | Salta el caso de uso y el límite transaccional |
| Devolver entidades JPA en REST | Expone el esquema y rompe el contrato |
| `BaseRepository<T, ID>` / `AbstractCrudService<T>` | Abstracción genérica sin valor; oculta reglas |
| `@Transactional` en controlador o repositorio | Límite transaccional difuso |
| `@ManyToOne` entre módulos | Acopla entidades y provoca lazy loading cruzado |
| `Open Session in View` | Mantiene conexiones y oculta problemas de carga |
| `double` para dinero | Pérdida de precisión |
| `Instant.now()` en dominio | Pruebas no deterministas |
| `catch (DataIntegrityViolationException)` en controlador | El adaptador debe traducir; el web no conoce SQL |
| Lombok/`@Data` en entidades | No está en el stack y genera igualdad/hashCode peligrosos |
| Eventos para todo | Complejidad sin efecto secundario real |

## 14. Ejemplo completo: registrar un movimiento

1. **HTTP**: `POST /api/v1/posiciones/{id}/movimientos` con
   `RegistrarMovimientoRequest` (tipo, fecha, cantidad, precio, importe, moneda,
   comisión, descripción).
2. **Validación de forma**: `@Valid` verifica campos obligatorios, longitudes y
   rangos simples.
3. **Adaptador web**: convierte a `RegistrarMovimientoCommand` y llama a
   `RegistrarMovimientoUseCase.ejecutar(...)`.
4. **Servicio de aplicación** (`@Transactional`):
   - carga la posición con `PosicionRepository.buscarParaActualizar(id)`;
   - verifica que la posición esté activa y que la cuenta/instrumento/moneda
     existan mediante puertos de consulta;
   - invoca `posicion.registrarMovimiento(...)`, que valida reglas y calcula la
     nueva cantidad/precio;
   - guarda la posición y el movimiento.
5. **Adaptador de persistencia**: `PosicionRepositoryAdapter` actualiza
   `PosicionEntity` e inserta `MovimientoEntity`; los mappers traducen.
6. **Commit**: si algo falla, no queda ni movimiento ni cambio de posición.
7. **Respuesta**: `201 Created`, `Location` y `MovimientoResponse` (no entidad).

## 15. Evolución

- **Si un módulo crece**: se separa en subproyecto Gradle con `api` e
  `implementation` para forzar límites en compilación.
- **Si aparece un caso de uso que escribe en dos módulos**: se diseña un
  orquestador explícito, eventos con compensación o se revisa el límite de
  módulos. No se permite un repositorio compartido.
- **Si aparece reportería**: se crea un read model/consulta dedicada; no se
  rompen los límites con joins entre tablas de módulos.
- **Si se aprueba la propuesta v2**: se actualizan mappers, entidades y pruebas
  en una tarea separada; este documento no asume esos cambios.
