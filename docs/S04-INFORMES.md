# S04 - Informes de resolucion (Marcelo)

Deje subido en mi rama todo lo de informes de resolucion (`POST /informes-resolucion`, `GET /informes-resolucion/{id}` y `GET /informes-resolucion` con filtros por `incidenteId` y `responsable`).

## Que incluye

- Cuando se registra un informe, se guarda el informe, se pasa el incidente a `RESUELTO` y se graba el cambio en el historial dentro de la misma transaccion y con exactamente la misma fecha y hora (usando el `Clock` del proyecto).
- Solo deja resolver incidentes que existan (`404`) y esten en estado `EN_PROCESO` (`409` en cualquier otro estado). Le puse bloqueo `PESSIMISTIC_WRITE` al incidente antes de resolverlo para que no se generen dos informes si entran dos pedidos juntos.
- La relacion entre informe e incidente es `@ManyToOne` asi si despues se reabre un incidente y se vuelve a resolver, no se pisan los informes anteriores.
- Como todavia no estaba subida la parte de S03, deje armadas las entidades base `Incidente` e `HistorialEstadoIncidente`, los enums acordados y el servicio interno `IncidenteResolucionService`. Cuando Leandro suba su parte conectamos directo con eso.

## Como probarlo local

1. Levantar la app desde Spring Tools para que Hibernate cree las tablas nuevas (`incidentes`, `historial_estados_incidente` e `informes_resolucion`).
2. Correr `scripts/02-datos-contratos.sql` si todavia no tienen el contrato `9001` en la base.
3. Correr `scripts/04-datos-prueba-informes.sql` para cargar los incidentes de prueba (`9201` al `9204`).
4. Importar en Postman la coleccion `pruebas/S04-Informes.postman_collection.json` y tirar los casos en orden (tambien se puede probar directo desde Swagger en `http://localhost:8080/swagger-ui.html`).

## Consulta para chequear en MySQL despues de probar

```sql
SELECT i.id AS incidente_id, i.estado, inf.id AS informe_id, inf.costo, inf.responsable, inf.fecha_resolucion
FROM incidentes i
LEFT JOIN informes_resolucion inf ON inf.incidente_id = i.id
WHERE i.id IN (9201, 9202, 9203, 9204)
ORDER BY i.id;

SELECT * FROM historial_estados_incidente
WHERE incidente_id IN (9201, 9202, 9203)
ORDER BY incidente_id, id;
```
