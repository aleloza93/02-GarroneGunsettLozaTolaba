# Equipo, les paso una propuesta para organizarnos con el TP02

Buenas! Dejé armada una primera base del proyecto y les comparto cómo propongo que nos organicemos para que después podamos integrar las cuatro partes sin complicarnos.

La idea no es dejar todo cerrado de antemano: revisemos estas convenciones y ajustemos lo que haga falta. Lo que sí tenemos que respetar son las reglas y responsabilidades de la consigna.

## Reparto que ya tenemos

| Integrante | Módulo |
| --- | --- |
| Javier | S01 - Facturas |
| Alejandro (yo) | S02 - Pagos |
| Leandro | S03 - Incidentes |
| Marcelo | S04 - Informes de resolución |

Vamos a trabajar sobre una sola aplicación y una base MySQL común al proyecto. Cada uno puede tener su instalación local, pero tenemos que compartir el modelo y los scripts para que funcione igual para todos.

## Cómo dejé organizada la base

Mantuve Java 21 y Spring Boot 4.1.2, siguiendo la configuración que mostró el profesor.

Propongo trabajar por módulo y, adentro, por capas:

- `controller`: recibe las solicitudes HTTP y devuelve las respuestas.
- `service`: contiene las reglas de negocio y las transacciones.
- `repository`: accede a la base de datos.
- `model`: contiene las entidades y enumeraciones.
- `dto`: define los datos de entrada y salida de la API.

Usamos Spring MVC, pero como es una API REST devolvemos JSON, no páginas HTML. La lógica del negocio queda en los servicios y no directamente en los controladores.

También dejé una consulta de contratos completa como ejemplo, manejo común de errores y configuración para las fechas. Los cuatro módulos del TP todavía están pendientes; los modelos de entrada de Facturas y Pagos son una propuesta inicial.

Por ahora usaría inyección por constructor y no agregaría Lombok, para mantener sencilla la configuración de todos.

## Convenciones que les propongo

- Campos Java y JSON en castellano sin acentos y con `camelCase`: por ejemplo, `fechaPago` y `contratoId`.
- Identificadores `Long`, generados por la base.
- Fechas con formato `AAAA-MM-DD` y períodos con `AAAA-MM`.
- Enumeraciones en mayúsculas y sin acentos.
- Importes con `BigDecimal`, no con `double` ni `float`.
- Para persistencia, propongo `DECIMAL(15,2)`. Los dos decimales son una decisión nuestra para acordar, no una exigencia textual del TP. No redondearía valores silenciosamente.
- Los IDs y los estados que administra el sistema no se reciben como campos del alta.
- Las entidades de la base no se devuelven directamente: usamos DTO de respuesta.

Para las fechas y horas usaríamos la zona `America/Argentina/Buenos_Aires` y el `Clock` compartido que dejé configurado. Así todas las partes consultan el mismo criterio de fecha y podemos probarlo con más facilidad.

## Estados y valores comunes

| Dato | Valores |
| --- | --- |
| Factura | PENDIENTE, VENCIDA, PAGADA, ANULADA |
| Incidente | ABIERTO, EN_PROCESO, RESUELTO, CANCELADO, REABIERTO |
| Medio de pago | TRANSFERENCIA, EFECTIVO, DEBITO, CREDITO, OTROS |
| Categoría de incidente | PLOMERIA, ELECTRICIDAD, GAS, GENERAL |
| Prioridad | BAJA, MEDIA, ALTA |
| Contrato auxiliar | BORRADOR, ACTIVO, FINALIZADO, RESCINDIDO |

Los nombres de estados del contrato son una propuesta de soporte. Para las altas de Facturas e Incidentes necesitamos validar especialmente que esté ACTIVO, además de las otras condiciones que pide cada módulo.

## Respuestas de la API

Para que las cuatro partes respondan de la misma manera:

- `200`: consultas y cambios de estado correctos.
- `201`: altas, con el recurso creado y el encabezado `Location`.
- `400`: campos faltantes, formatos o valores inválidos.
- `404`: recurso solicitado o relacionado que no existe.
- `409`: conflictos del negocio, como duplicados o estados incompatibles.

Dejé un manejo común de errores con `ProblemDetail`. Los errores de validación también incluyen una lista de campos y mensajes. En los futuros controladores tenemos que usar `@Valid` para activar las validaciones de los DTO.

Cada módulo tiene que traducir sus conflictos conocidos de base de datos al error correspondiente. No trataría cualquier error SQL como un `409`: una falla de infraestructura es otra cosa. Tampoco tenemos que devolver claves, trazas o SQL al cliente.

### Enlaces HATEOAS

Todos los recursos llevan `self`. Además:

- Factura: enlace al contrato y, cuando exista, al pago.
- Pago: enlace a la factura.
- Incidente: enlaces al contrato y a sus informes.
- Informe: enlace al incidente.

Para listados propongo `CollectionModel`, con un enlace `self` que conserve los filtros utilizados. Para errores también dejé un enlace a la ruta de la solicitud.

## Contratos como soporte común

Ya hay una consulta `GET /contratos/{id}` y un modelo básico con:

`id, fechaInicio, duracionMeses, importeMensual, diaVencimiento, estado, eliminado`.

La fecha de fin se calcula sumando la duración a la fecha de inicio y restando un día.

La consulta permite ver contratos finalizados o eliminados para conservar la navegación histórica. Eso no significa que se puedan usar para emitir nuevas facturas o registrar incidentes: cada módulo debe validar las condiciones de su alta.

Por ahora no armé el CRUD completo de contratos ni la gestión de propiedades, personas o publicaciones.

## Operaciones que tenemos que coordinar bien

Este es el punto más importante para integrar:

- **Mi parte, Pagos:** guardar el pago y pasar la factura a PAGADA dentro de la misma transacción.
- **Leandro, Incidentes:** cambiar el estado y registrar el historial dentro de la misma transacción.
- **Marcelo, Resolución:** guardar el informe, resolver el incidente y registrar el historial dentro de la misma transacción.

Si falla alguna de esas acciones, no debe guardarse ninguna de las que forman parte de esa operación.

Propongo que nos comuniquemos con llamadas internas entre servicios Java, sin hacer llamadas HTTP entre módulos de esta misma aplicación. Tampoco separar esas escrituras con `REQUIRES_NEW`.

Además de validar antes de guardar, tenemos que acordar cómo protegernos de solicitudes simultáneas mediante bloqueos, versionado y restricciones en la base.

Con Javier revisamos Factura–Pago; Leandro y Marcelo pueden acordar Incidente–Historial–Informe. Marcelo implementa la resolución completa y Leandro no expone el paso a RESUELTO por el cambio de estado común.

## Tareas compartidas

Además de cada módulo, tenemos que repartir configuración, datos de prueba, consultas auxiliares e integración. Les propongo esto, sujeto a que lo charlemos:

- **Javier:** criterios de facturación y sus datos y filtros de prueba.
- **Yo:** configuración inicial, coordinación con Javier y recorrido integrado de facturación y pago.
- **Leandro:** revisar el soporte de contratos necesario para incidentes, historial y sus datos de prueba.
- **Marcelo:** revisar con el equipo los enlaces HATEOAS y coordinar el recorrido integrado de mantenimiento.
- **Todos:** revisar los acuerdos, integrar avances y aportar las pruebas de su módulo.

No cambia el reparto principal; es para que las tareas comunes también tengan responsables.

## Cómo seguiría

1. Que todos podamos importar y ejecutar la base en Spring Tools con MySQL.
2. Revisar los modelos compartidos por parejas. (Ale Javi - Marce Lea)
3. Avanzar en una rama por módulo e integrar cambios chicos, revisados por otro compañero.
4. Probar pronto los dos recorridos completos, sin esperar al final.
5. Completar los casos negativos, las pruebas de operaciones simultáneas y la verificación de que no queden registros parciales.
6. Preparar la entrega con scripts y pruebas reproducibles.

Cada uno tiene que aportar al menos seis casos: operación exitosa, consulta por ID, listado filtrado, datos inválidos, recurso inexistente y conflicto de negocio. A eso sumamos los dos recorridos integradores.

No subamos contraseñas ni configuraciones personales al repositorio. La guía para ejecutar la base está en el README.

Un detalle para no confundirnos: los IDs S01 a S04 son los de esta actividad, no los números del catálogo que habíamos armado en el TP01.

Díganme cómo lo ven y si cambiarían algo. La idea es acordar esta base entre todos y después avanzar tranquilos con cada parte.

Ale

