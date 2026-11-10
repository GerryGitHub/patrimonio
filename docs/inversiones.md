# Catálogo de instrumentos y posiciones (Entrega 2)

Este documento describe el módulo `inversiones` implementado en la Entrega 2:
catálogo global de instrumentos financieros y posiciones iniciales dentro de
cuentas financieras. No incluye movimientos, compras, ventas, contratos de
inversión a plazo ni valuaciones de mercado.

## Alcance

- Catálogo compartido de instrumentos de renta variable, renta fija y activos
  digitales.
- Registro de posiciones iniciales con cantidad, costo unitario y fecha de
  apertura.
- Consulta de posiciones por portafolio con filtros por cuenta, instrumento,
  categoría y estado.
- Agregados de costo por moneda y distribución por categoría para el dashboard.
- Sin valor de mercado, rendimiento, ganancia/pérdida no realizada ni
  patrimonio actualizado a precios de mercado.

## Modelo de dominio

| Concepto | Responsabilidad |
| --- | --- |
| `Instrumento` | Activo financiero del catálogo global. Agregado independiente. |
| `Posicion` | Tenencia inicial de un instrumento dentro de una cuenta. |
| `ClaseActivo` | Categoría extensible: renta variable, renta fija, activos digitales. |
| `TipoInstrumento` | Tipo dentro de una clase: acción, ETF, cete, criptomoneda, etc. |

Decisiones relevantes:

- El catálogo de instrumentos es **global**, igual que las instituciones. Cualquier
  usuario autenticado puede consultarlo y administrarlo; no pertenece a un
  usuario ni a un portafolio.
- La posición pertenece a la **cuenta**, no directamente al portafolio. El
  portafolio se determina a través de la cuenta.
- Una cuenta puede tener varias posiciones y un instrumento puede existir en
  varias cuentas.
- La moneda de la posición se deriva de la moneda del instrumento; no se solicita
  en el formulario.
- `posicion.precio_promedio` almacena el costo unitario. El costo total se
  calcula como `cantidad × costo unitario` y se expone con escala decimal.
- Los importes y cantidades usan `BigDecimal`/`NUMERIC`; nunca `double` ni
  `float`. Las respuestas JSON serializan estos valores como cadenas para
  conservar la precisión.
- La cantidad y el costo de una posición se pueden corregir mientras no exista el
  motor de movimientos. Cuando ese módulo exista, la posición quedará derivada
  de sus movimientos y esta edición se restringirá.

## Compatibilidad cuenta / instrumento

Las reglas son conservadoras y usan el nombre del tipo de cuenta:

| Tipo de cuenta | Clases admitidas |
| --- | --- |
| Cuenta a plazo | Ninguna posición bursátil: corresponde a contratos de la siguiente entrega. |
| Cuenta de exchange o cripto | Solo activos digitales. |
| Cuenta de corretaje, bolsa o inversión | Renta variable y renta fija; no activos digitales. |
| Cuenta corriente, ahorro u otra | Cualquier clase. |

Las cuentas inactivas no admiten posiciones nuevas. La validación se realiza en
el backend; el frontend solo muestra el error devuelto.

## Tablas y migración

Se reutilizan las tablas existentes `instrumento`, `identificador_instrumento` y
`posicion`. Los cambios son aditivos:

- `instrumento`: columnas `mercado`, `identificador_externo`,
  `fecha_creacion`, `fecha_actualizacion`; índice por `mercado`.
- `identificador_instrumento`: índice único parcial para ISIN activo.
- `posicion`: columna `observaciones`, `fecha_creacion`; restricción
  `ck_posicion_cantidad`; índice `(cuenta_id, instrumento_id)`.
- `db/datos-iniciales.sql`: semillas idempotentes de clases de activo y tipos de
  instrumento.

Para una base existente:

```bash
psql -v ON_ERROR_STOP=1 -f db/esquema-entrega2.sql
psql -v ON_ERROR_STOP=1 -f db/datos-iniciales.sql
```

`db/esquema-entrega2.sql` es aditivo e idempotente. `db/esquema.sql` ya contiene
el esquema completo para instalaciones nuevas. No se introduce Flyway ni se
eliminan datos.

Los pagarés y depósitos a plazo de bancos o SOFIPOs **no** se modelan como
posiciones bursátiles: corresponden a contratos de inversión en la siguiente
entrega.

## API

Los endpoints solo se registran con un perfil `local` o `test` activo de forma
explícita y `patrimonio.api.inversiones.expuesta=true`. En `prod` responden
`404` por diseño.

### Instrumentos

```http
GET   /api/v1/instrumentos?busqueda=&claseActivoId=&tipoInstrumentoId=&activo=
GET   /api/v1/instrumentos/categorias
GET   /api/v1/instrumentos/tipos?claseActivoId=
GET   /api/v1/instrumentos/{id}
POST  /api/v1/instrumentos
PUT   /api/v1/instrumentos/{id}
PATCH /api/v1/instrumentos/{id}/estado
```

### Posiciones

```http
GET   /api/v1/portafolios/{portafolioId}/posiciones?cuentaId=&instrumentoId=&claseActivoId=&activo=
GET   /api/v1/portafolios/{portafolioId}/posiciones/resumen
GET   /api/v1/portafolios/{portafolioId}/posiciones/{id}
POST  /api/v1/portafolios/{portafolioId}/posiciones
PUT   /api/v1/portafolios/{portafolioId}/posiciones/{id}
PATCH /api/v1/portafolios/{portafolioId}/posiciones/{id}/estado
```

`POST` y `PUT` de posición no permiten cambiar la cuenta ni el instrumento; solo
cantidad, costo unitario, fecha de apertura y observaciones. La respuesta incluye
`costoTotal`, calculado por el backend.

## Seguridad

- Sesión de servidor y CSRF con el mismo esquema del resto del proyecto.
- Autorización por membresía del portafolio:
  - `PROPIETARIO`, `ADMINISTRADOR` y `EDITOR`: lectura y escritura.
  - `LECTOR`: solo lectura.
- Sin membresía, portafolio inexistente o inactivo: `404` para no revelar su
  existencia.
- La cuenta se valida contra el portafolio solicitado; una cuenta de otro
  portafolio responde `404`.
- El usuario siempre se toma de la sesión, nunca del cuerpo.

## Frontend

Pantallas principales:

- `/instrumentos`: listado, búsqueda, filtros por categoría, tipo y estado, y alta.
- `/instrumentos/nuevo`, `/instrumentos/{id}`, `/instrumentos/{id}/editar`.
- `/activos/liquidez/{portafolioId}/posiciones`: listado, filtros, resumen de costo
  por moneda y distribución.
- `/activos/liquidez/{portafolioId}/posiciones/nueva`,
  `.../posiciones/{id}` y `.../posiciones/{id}/editar`.

El formulario de posición muestra el costo total como ayuda visual, agrupa los
instrumentos por categoría y adapta las etiquetas a la categoría seleccionada. El
costo agregado se presenta siempre por moneda; nunca se suman monedas distintas.
El dashboard usa posiciones reales para conteos, costo agregado y distribución,
sin presentar el costo como valor de mercado.

## Pruebas

- Backend: pruebas unitarias de dominio, servicios y validadores; pruebas de
  integración con PostgreSQL/Testcontainers para instrumentos, posiciones,
  filtros, duplicados, roles, aislamiento y precisión decimal.
- Frontend: pruebas de clientes de API, utilidades decimales, combinación de
  resúmenes, componentes de costo y tabla de posiciones; además de `typecheck` y
  `build`.

## Decisiones pendientes para la siguiente entrega

- Implementar el motor de movimientos (compras, ventas, transferencias) y derivar
  cantidad/costo de la posición desde ese historial.
- Modelar contratos de inversión a plazo (pagarés, depósitos) sin mezclarlos con
  posiciones bursátiles.
- Incorporar cotizaciones y valuaciones históricas para valor de mercado y
  rendimiento.
- Definir una clasificación funcional de tipos de cuenta en el catálogo para
  reemplazar las reglas por nombre.
