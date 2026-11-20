# S03 - Incidentes (Leandro)

Módulo de gestión de incidentes de mantenimiento: alta, consulta, listado con filtros, cambios de estado e historial. Se apoya en la estructura base que dejó armada S04 (entidades `Incidente` e `HistorialEstadoIncidente`, enums y repositorios) y no modifica ningún archivo de S04.

## Endpoints

| Método | Ruta | Descripción | Códigos |
|---|---|---|---|
| POST | `/incidentes` | Registrar un incidente | 201, 400, 404, 409 |
| GET | `/incidentes/{id}` | Consultar un incidente | 200, 404 |
| GET | `/incidentes` | Listar con filtros opcionales `contratoId`, `estado`, `categoria`, `prioridad` | 200 |
| PATCH | `/incidentes/{id}/estado` | Cambiar el estado según las transiciones permitidas | 200, 400, 404, 409 |
| GET | `/incidentes/{id}/historial` | Historial de estados en orden cronológico | 200, 404 |

Ejemplo de alta:

```json
{
  "contratoId": 9001,
  "titulo": "Pérdida de agua en lavadero",
  "descripcion": "Gotea la conexión de la canilla del lavarropas",
  "categoria": "PLOMERIA",
  "prioridad": "ALTA"
}
```

Ejemplo de cambio de estado: `{ "estado": "EN_PROCESO" }`

## Reglas implementadas

- **Contrato:** si no existe, devuelve `404`. Si está eliminado, no está `ACTIVO` o la fecha de alta queda fuera de su vigencia, devuelve `409`. La vigencia termina en fecha de inicio + duración en meses − 1 día.
- **Alta:** el incidente se crea en estado `ABIERTO` y se genera la primera entrada del historial, sin estado anterior y con la misma fecha y hora del alta.
- **Transiciones permitidas en S03:**

  | Estado actual | Puede pasar a |
  |---|---|
  | ABIERTO | EN_PROCESO, CANCELADO |
  | EN_PROCESO | CANCELADO |
  | RESUELTO | REABIERTO |
  | REABIERTO | EN_PROCESO |
  | CANCELADO | (ninguno) |

- **Resolución:** el pase `EN_PROCESO → RESUELTO` por PATCH se rechaza con `409`, porque solo se hace con `POST /informes-resolucion` (S04).
- **Historial:** cada cambio guarda estado anterior, estado nuevo y fecha/hora en la misma transacción que la actualización del estado.
- **Concurrencia:** el PATCH bloquea el incidente (`PESSIMISTIC_WRITE`), igual que S04 al resolver, para que un cambio de estado y una resolución simultáneos no se pisen.
- **Reapertura:** reabrir no borra los informes anteriores, porque S03 nunca toca `informes_resolucion`.
- **Contrato finalizado después del alta:** el seguimiento continúa, porque el contrato solo se valida al registrar el incidente.

## HATEOAS

- **Incidente:** `self`, `contrato` (`/contratos/{id}`), `informes` (`/informes-resolucion?incidenteId={id}`) e `historial`.
- **Historial:** `self` e `incidente`.
- **Listados:** `self`. Si no hay resultados, devuelven igual `_embedded` con la lista vacía, como en S04.

## Archivos nuevos

```
src/main/java/com/tuti/daos/controller/IncidenteController.java
src/main/java/com/tuti/daos/dto/RegistrarIncidenteRequest.java
src/main/java/com/tuti/daos/dto/CambiarEstadoIncidenteRequest.java
src/main/java/com/tuti/daos/dto/IncidenteResponse.java
src/main/java/com/tuti/daos/dto/HistorialEstadoResponse.java
src/main/java/com/tuti/daos/repository/IncidenteRepository.java
src/main/java/com/tuti/daos/service/IncidenteService.java
src/main/java/com/tuti/daos/service/impl/IncidenteServiceImpl.java
src/test/java/com/tuti/incidentes/IncidenteApiTests.java
scripts/03-datos-prueba-incidentes.sql
pruebas/S03-Incidentes.postman_collection.json
docs/S03-INCIDENTES.md
```

## Cómo probarlo local

1. Levantar la app desde Spring Tools.
2. Correr `scripts/02-datos-contratos.sql` si todavía no tienen el contrato `9001`.
3. Correr `scripts/03-datos-prueba-incidentes.sql`, que carga los incidentes `9101` a `9104`:

   | Incidente | Estado | Detalle |
   |---|---|---|
   | 9101 | ABIERTO | |
   | 9102 | EN_PROCESO | |
   | 9103 | RESUELTO | con su informe |
   | 9104 | CANCELADO | |

4. Importar en Postman `pruebas/S03-Incidentes.postman_collection.json` y ejecutar los casos en orden. La carpeta **Recorrido integrador de mantenimiento** cubre el recorrido completo que pide la consigna: alta, en proceso, resolución, reapertura, en proceso, segunda resolución, rechazo de un tercer informe, los dos informes conservados y el historial completo.

También se puede probar desde Swagger en `http://localhost:8080/swagger-ui.html`.

## Consulta para chequear en MySQL

```sql
SELECT i.id, i.estado, h.estado_anterior, h.estado_nuevo, h.fecha_hora
FROM incidentes i
JOIN historial_estados_incidente h ON h.incidente_id = i.id
WHERE i.id BETWEEN 9101 AND 9199 OR i.id > 9204
ORDER BY i.id, h.fecha_hora, h.id;
```
