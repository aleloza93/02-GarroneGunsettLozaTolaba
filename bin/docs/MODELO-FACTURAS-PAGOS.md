# Colo, te paso propuesta para coordinar Facturas y Pagos


La idea es definir los campos, las relaciones y cómo vamos a conectar las dos partes. Tenemos que respetar las reglas que pide la consigna si os si

Ya dejé una primera base con consulta de contratos, modelos de entrada (DTO) y enumeraciones. Todavía no están implementados los endpoints ni las tablas de Facturas y Pagos ni nada xD

## Cómo nos repartimos

- Vos te encargás de emitir, consultar, filtrar y anular facturas.
- Yo me encargo de registrar y consultar pagos, incluyendo que la factura quede pagada cuando se registra el pago.

El punto más importante es que **guardar el pago y cambiar la factura a PAGADA tiene que ser una sola operación**: si algo falla, no se guarda ninguna de las dos cosas.

## Relaciones que pienso

- Un contrato puede tener varias facturas.
- Cada factura pertenece a un contrato.
- Cada factura puede tener como máximo un pago.
- Cada pago corresponde a una única factura.

Para ese último punto, además de validarlo en Java, necesitamos una restricción `UNIQUE` sobre `pagos.factura_id` en MySQL, como pide el TP, porque sino se puede hacer bardo.

## Modelo de Factura

Pienso estos campos:

| Campo | Tipo | Uso |
| --- | --- | --- |
| id | Long | Generado automáticamente |
| contrato | Contrato | Relación obligatoria; por JSON recibimos contratoId |
| periodo | YearMonth | Por JSON usamos AAAA-MM; falta definir la conversión para guardarlo |
| tipo | TipoFactura | MENSUAL o ADICIONAL, definido por el sistema |
| concepto | String | Opcional |
| fechaEmision | LocalDate | Si no viene, usamos la fecha actual |
| fechaVencimiento | LocalDate | Si no viene, se calcula con el período y el día de vencimiento del contrato |
| importe | BigDecimal | Se copia al emitir y después no cambia |
| estado | EstadoFactura | PENDIENTE, VENCIDA, PAGADA o ANULADA |
| version | Long | Propuesta con @Version para controlar modificaciones simultáneas |

Hay algunos detalles de la consigna que tenemos que cuidar:

- Si no mandan importe, lo tomamos del contrato y la factura es mensual.
- Si mandan importe, es adicional, aunque coincida con el importe mensual.
- No puede haber dos mensuales no anuladas para el mismo contrato y período.
- Si se anula una mensual, se puede emitir otra conservando la anterior.
- Cambiar el importe del contrato no modifica las facturas ya emitidas.
- Si el día de vencimiento no existe en ese mes, se usa el último día.

Por eso, no pondría una restricción única general sobre contrato y período: también bloquearía las adicionales y el reemplazo de una mensual anulada.

Para evitar que dos solicitudes creen la misma mensual al mismo tiempo, te propongo bloquear la fila del contrato mientras se verifica y se emite. Lo tenemos que probar en MySQL; consultar si existe y después guardar, sin protección, no alcanza.

### Cómo manejaríamos el vencimiento

Me parece práctico guardar PENDIENTE, PAGADA o ANULADA y calcular VENCIDA cuando corresponda: si no está pagada ni anulada y ya pasó su fecha de vencimiento.

La consigna permite hacerlo así, sin un proceso programado. Eso sí: los listados y filtros tienen que aplicar la misma regla. Te propongo que esa lógica quede centralizada en tu parte, así no la duplicamos.

El importe base no cambia por vencer. El interés se informa al registrar el pago.

### Ejemplos de lo que recibiríamos

Para emitir una mensual, con `POST /facturas`:

```json
{
  "contratoId": 9001,
  "periodo": "2026-11"
}
```

Para una adicional:

```json
{
  "contratoId": 9001,
  "periodo": "2026-11",
  "concepto": "Reparación a cargo del inquilino",
  "fechaEmision": "2026-11-01",
  "fechaVencimiento": "2026-11-10",
  "importe": 35000.00
}
```

Son ejemplos para acordar el formato. La emisión no puede ser posterior al vencimiento; si facturamos un período pasado, puede hacer falta informar una fecha válida en lugar de usar hoy.

Para anular, con `PATCH /facturas/{id}/estado`:

```json
{
  "estado": "ANULADA"
}
```

Ese endpoint no permitiría poner PAGADA ni cualquier otro estado. Una factura pagada tampoco se puede anular.

## Modelo de Pago

De mi lado propongo esto:

| Campo | Tipo | Uso |
| --- | --- | --- |
| id | Long | Generado automáticamente |
| factura | Factura | Obligatoria y única; por JSON recibimos facturaId |
| fechaPago | LocalDate | Si no viene, usamos hoy; no puede ser anterior a la emisión ni futura |
| medioPago | MedioPago | TRANSFERENCIA, EFECTIVO, DEBITO, CREDITO u OTROS |
| importePagado | BigDecimal | Positivo e incluye el interés |
| interes | BigDecimal | Obligatorio, mayor o igual a cero |

Por ejemplo, con `POST /pagos`:

```json
{
  "facturaId": 1,
  "medioPago": "TRANSFERENCIA",
  "importePagado": 255000.00,
  "interes": 5000.00
}
```

Si la factura es de 250000.00, ese pago cubre el total. El ID es ilustrativo; para las pruebas usamos el que devuelva tu alta.

Voy a validar que:

`importePagado = importeFactura + interes`

No tenemos que calcular el interés automáticamente ni sumarlo al importe guardado de la factura. Para comparar los importes usaría `BigDecimal.compareTo`, así no tenemos problemas por diferencias de escala.

## Cómo conectaríamos nuestras partes (turbio)

Pienso que tu servicio tenga una operación interna para obtener y bloquear una factura, y otra para marcarla pagada dentro de la transacción que abre mi `PagoService`.

Yo coordinaría estos pasos:

1. Validar los datos y abrir la transacción.
2. Obtener la factura con bloqueo para escritura.
3. Verificar que esté pendiente o vencida, que no tenga pago y que las fechas sean válidas.
4. Comprobar el importe total.
5. Guardar el pago y marcar la factura como pagada.
6. Confirmar todo junto; si algo falla, deshacer ambas escrituras.

Lo haríamos con llamadas internas entre servicios Java, no llamando a tu API por HTTP.

Para anular tendríamos que usar el mismo bloqueo de factura. Así evitamos que, si entran una anulación y un pago al mismo tiempo, termine quedando una factura anulada con un pago registrado.

Los conflictos de negocio los devolveríamos como `409`. La restricción de un solo pago también queda protegida en la base.

## Endpoints y consultas

| Tu parte: Facturas | Mi parte: Pagos |
| --- | --- |
| POST /facturas | POST /pagos |
| GET /facturas/{id} | GET /pagos/{id} |
| GET /facturas | GET /pagos |
| PATCH /facturas/{id}/estado | No se modifican ni eliminan pagos |

Para filtros, propongo `contratoId`, `periodo` y `estado` en Facturas, combinables. En Pagos voy a incluir `facturaId`, que lo pide la consigna. Si no hay coincidencias, devolvemos una colección vacía con `200`.

Las altas devolverían `201`, con `Location` y el recurso creado. Las consultas y la anulación, `200`.

En los enlaces HATEOAS:

- La factura tiene `self`, `contrato` y `pago` solamente cuando exista.
- El pago tiene `self` y `factura`.

Dejaría la paginación fuera de esta primera versión, es sumar una complejidad que veo que no esta pedida.

## Qué probaríamos juntos

- Emitir una mensual, pagarla y consultar ambos recursos mediante sus enlaces.
- Rechazar una mensual duplicada y permitir reemplazar una anulada.
- Permitir adicionales sin confundirlas con mensuales.
- Verificar vencimientos y filtros de facturas vencidas.
- Rechazar pagos parciales, duplicados o de facturas anuladas.
- Aceptar interés cero y rechazar interés ausente o negativo.
- Rechazar fechas de pago inválidas.
- Comprobar que una falla no deje el pago o la factura guardados a medias.
- Probar dos pagos simultáneos y un pago simultáneo con una anulación.

Después cada uno completa sus seis casos reproducibles y armamos el recorrido integrado que pide el TP.

Fijate cómo lo ves zurdo. Si te cierra esta base, acordamos los nombres de los métodos internos y avanzamos cada uno con su parte. Si preferís organizar algo distinto, lo ajustamos antes de seguir.

No te duermas?)
