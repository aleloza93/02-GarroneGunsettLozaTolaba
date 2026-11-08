# S02 - Pagos: implementación de Alejandro

## Estado al 7 de noviembre de 2026

Implementado en la rama Alejandro, con la base remota incorporada.
No incluye los endpoints de Facturas ni reemplaza el trabajo de Javier.

### Operaciones disponibles

| Método y ruta | Resultado |
| --- | --- |
| POST /pagos | 201, pago creado, Location y enlaces self/factura |
| GET /pagos/{id} | 200, pago con sus enlaces; 404 si no existe |
| GET /pagos | 200, listado ordenado por ID |
| GET /pagos?facturaId=9101 | 200, solo el pago de esa factura |
| GET /pagos?facturaId=999999 | 200, colección vacía si no hay pago |

No se exponen modificación ni eliminación de pagos.

### Reglas implementadas

- Validaciones de campos obligatorios, IDs positivos, medio de pago y decimales.
- Interés obligatorio, no negativo; importe pagado positivo.
- Fecha omitida: fecha actual según el Clock del proyecto.
- Fecha de pago entre la emisión de la factura y hoy, inclusive.
- Factura existente y pendiente o vencida; no se pagan anuladas ni pagadas.
- Importe pagado exactamente igual al importe base más interés.
- No se calcula automáticamente el interés ni se modifica el importe base.
- Pago y cambio de estado guardados en una misma transacción.
- Bloqueo de la factura para serializar registros simultáneos.
- Restricción UNIQUE sobre factura_id en pagos, también en la base.
- No se exige contrato activo al pagar una factura ya emitida.

Datos inválidos devuelven 400, recursos inexistentes 404 y conflictos de
negocio 409. Los listados vacíos incluyen explícitamente _embedded.pagos: [].

## Archivos principales

- pagos/controller/PagoController: operaciones HTTP y enlaces.
- pagos/service/PagoService: reglas y transacción completa.
- pagos/model/Pago: tabla pagos y relación única con factura.
- pagos/repository/PagoRepository: persistencia y filtro.
- pagos/dto: entrada y respuesta.
- common/exception/DatosInvalidosException: errores de fechas/IDs.

Todos los paquetes están bajo src/main/java/com/tuti.

## Soporte compartido agregado para poder avanzar

Como S01 aún no está implementado en el repositorio, se incorporó el modelo
Factura acordado: contrato, período, tipo, concepto, emisión, vencimiento,
importe, estado y versión. PeriodoConverter persiste YearMonth como AAAA-MM.

FacturaPagoRepository y FacturaPagoService son el acceso interno usado por S02:
- obtenerParaPago(id) obtiene la factura con PESSIMISTIC_WRITE.
- marcarPagada(factura) actualiza el estado y hace flush.
- Ambas operaciones exigen una transacción existente (MANDATORY).
- PagoService abre esa transacción, guarda el pago y llama a marcarPagada.

Se conservó sin cambios el archivo facturaRepository.java que subió Javier.
Al integrar S01, podemos consolidar el repositorio sin duplicar el modelo ni
crear otra tabla. No se implementó emisión, anulación ni consulta REST de S01.

Factura.estadoEn(fecha) centraliza el estado efectivo: pagada/anulada se
conservan; en las otras, se compara la fecha con el vencimiento. S01 debe usar
el mismo criterio en respuestas y filtros.

### Pendientes con Javier

1. Adoptar o integrar la entidad Factura compartida.
2. Completar la emisión y sus validaciones en S01.
3. Utilizar el mismo bloqueo de fila al anular; no actualizar estado por SQL
   directo ni sin control concurrente desde S01.
4. No permitir PAGADA a través de PATCH; solo mediante S02.
5. Implementar GET /facturas/{id}, incluyendo el enlace al pago cuando exista.
6. Probar el recorrido completo de emisión, pago y navegación HATEOAS.
7. Probar el pago simultáneo con una anulación real de S01.

**El enlace factura de las respuestas de S02 está generado, pero su destino
todavía no funciona porque falta el controlador de S01.** Los casos de S02
verifican el enlace, no presentan el recorrido integrado como terminado.

## Cómo probar en Spring Tools y MySQL

### Swagger UI (agregado el 8 de noviembre)

Se incorporó SpringDoc 3.2.0, compatible con Spring Boot 4.1.x según la
[matriz oficial](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot).
En STS: refrescar con F5, Maven > Update Project y reiniciar la aplicación.

- Interfaz: http://localhost:8080/swagger-ui.html
- Especificación JSON: http://localhost:8080/v3/api-docs
- Secciones actuales: Pagos y Contratos. No se documentan como disponibles
  los endpoints de Facturas que todavía faltan implementar.
- Primero probar GET /pagos/1 o GET /contratos/9001 con Try it out > Execute.
- Un POST desde Swagger guarda datos reales. La factura 9101 ya pagada
  devuelve 409; no borrar el pago para repetir el ejemplo.
- El ejemplo del alta usa 9102 con interés: solo funcionará si esa factura
  existe y todavía no tiene pago.

Las anotaciones explican las operaciones; no reemplazan las validaciones,
las pruebas automáticas ni los casos reproducibles de Postman.

1. Estar en la rama Alejandro y actualizar/refrescar el proyecto.
2. Reiniciar la aplicación con las variables DB_USER, DB_PASSWORD y DB_URL ya
   configuradas. Hibernate agregará facturas y pagos; no usar create-drop en
   la base del TP.
3. Ejecutar scripts/02-datos-contratos.sql si aún no están los contratos.
4. Revisar y ejecutar scripts/03-datos-prueba-pagos.sql.
5. Importar pruebas/S02-Pagos.postman_collection.json en Postman.
6. Dejar baseUrl=http://localhost:8080 y ejecutar los casos en orden.

El script usa IDs reservados 9101 a 9104 y no sobrescribe datos ni borra pagos.
Las facturas son fixtures ADICIONAL: no simulan que S01 haya emitido facturas.
La fecha del servidor debe ser 2026-11-01 o posterior para los casos exitosos.

La colección guarda automáticamente el ID del primer pago en pagoId.
Los casos 01 a 06 cubren los seis requisitos mínimos por integrante.
Los demás prueban parcial, anulada, fecha inválida, interés y ausencia de pagos
parciales. Cada caso incluye preparación, resultado esperado y aserciones HTTP.

**Repetición:** luego de pagar las facturas 9101 y 9102, volver a ejecutar el
script no las reinicia. Un nuevo POST debe devolver 409. Para repetir los casos
de alta desde cero, usar una base de pruebas nueva o nuevos IDs de fixtures;
no borrar pagos en la base compartida. Las pruebas automáticas sí recrean sus
datos exclusivamente en su base aislada.

### Verificación en MySQL tras ejecutar Postman

```sql
SELECT f.id, f.estado, f.importe, p.id AS pago_id,
       p.importe_pagado, p.interes
FROM facturas f
LEFT JOIN pagos p ON p.factura_id = f.id
WHERE f.id IN (9101, 9102, 9103, 9104)
ORDER BY f.id;

SHOW CREATE TABLE pagos;
```

Resultado esperado:
- 9101: PAGADA, base 250000, un pago de 250000 e interés 0.
- 9102: PAGADA, base 250000, un pago de 255000 e interés 5000.
- 9103: ANULADA, sin pago.
- 9104: PENDIENTE en almacenamiento, sin pago; su estado efectivo dependerá
  de la fecha al consultar por S01.
- Tabla pagos: restricción única sobre factura_id.

## Pruebas automáticas

Verificación realizada el 7 de noviembre de 2026:

- Compilación y empaquetado correctos; 49 pruebas aprobadas en la suite completa.
- Las 35 pruebas de Pagos también aprobadas contra MySQL 8.4.13 aislado,
  incluyendo concurrencia, restricción única y rollback.
- Los tres scripts SQL ejecutados en esa instancia temporal.
- Las 13 solicitudes de la colección ejecutadas por HTTP contra el JAR:
  todos los códigos esperados y verificaciones de datos resultaron correctos.
  Se ejecutaron desde un verificador local, no desde la interfaz de Postman.
- Comprobados en SQL los estados, importes y ausencia de pagos para los rechazos.
- No se modificó la base habitual del TP. Los procesos de prueba se detuvieron.

La verificación usó el JDK 25 incluido en STS para compilar con release 21.
Al iniciar el JAR, ese JDK presentó un problema de sockets locales con la ruta
temporal abreviada de Windows. Se pudo ejecutar indicando temporalmente
`-Djdk.net.unixdomain.tmpdir=C:/ws/daos/02-GarroneGunsettLozaTolaba/target`.
Esto fue una opción del proceso de prueba: no se cambió Java 21 en el proyecto
ni se modificaron las configuraciones de ejecución de STS.

```powershell
.\mvnw.cmd test
```

Por defecto usan H2 temporal y un reloj fijo para los casos de Pagos. No leen
credenciales de STS ni se conectan a la base MySQL del TP.

PagoApiTests cubre altas, consultas, filtros, validaciones, enlaces, duplicados,
restricción en base de datos, dos pagos simultáneos y rollback inducido después
de hacer flush de las dos escrituras. El fallo inducido existe solo en pruebas;
no hay un endpoint de fallos en producción.

### Repetir S02 en MySQL aislado (opcional)

Se puede ejecutar la misma suite contra una instancia **temporal y descartable**
MySQL 8.4, exclusivamente en 127.0.0.1:13307, esquema hogarya_s02_test.
El modo está preparado para root sin contraseña SOLO en esa instancia efímera,
limitada al equipo local; no usar esa configuración en el servidor habitual.

```powershell
.\mvnw.cmd test -Dtest=PagoApiTests -Ds02.mysql=true
```

ADVERTENCIA: este modo usa create-drop y reinicia los datos de pruebas.
Preparar antes una instancia temporal independiente y el esquema indicado,
y detenerla al terminar. No apuntarlo al servidor ni al esquema del TP.
Sin la opción s02.mysql, las pruebas siguen usando H2.

Referencias técnicas usadas: [bloqueos en Spring Data JPA](https://docs.spring.io/spring-data/jpa/reference/4.0/jpa/locking.html),
[espías de pruebas en Spring](https://docs.spring.io/spring-framework/reference/testing/annotations/integration-spring/annotation-mockitobean.html)
y [colecciones HAL vacías](https://docs.spring.io/spring-hateoas/docs/current/api/org/springframework/hateoas/server/core/EmbeddedWrappers.html).
