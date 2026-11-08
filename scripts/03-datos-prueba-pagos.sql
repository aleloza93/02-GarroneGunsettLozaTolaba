-- Fixtures de S02, no reemplazan la emision por S01.
-- Ejecutar manualmente en MySQL DESPUES de arrancar esta version y de 02-datos-contratos.sql.
-- Reservar IDs 9101..9104 para pruebas. No borra ni modifica registros existentes.
-- Si ya existen, revisar que sus datos correspondan; no se resetean pagos.
USE `02-GarroneGunsettLozaTolaba`;

START TRANSACTION;

INSERT INTO facturas
    (id, contrato_id, periodo, tipo, concepto, fecha_emision, fecha_vencimiento, importe, estado, version)
SELECT 9101, 9001, '2026-11', 'ADICIONAL', 'S02 pago exitoso y duplicado', '2026-11-01', '2026-11-10', 250000.00, 'PENDIENTE', 0
WHERE EXISTS (SELECT 1 FROM contratos WHERE id = 9001)
  AND NOT EXISTS (SELECT 1 FROM facturas WHERE id = 9101);

INSERT INTO facturas
    (id, contrato_id, periodo, tipo, concepto, fecha_emision, fecha_vencimiento, importe, estado, version)
SELECT 9102, 9001, '2026-10', 'ADICIONAL', 'S02 vencida con interes', '2026-10-01', '2026-10-10', 250000.00, 'PENDIENTE', 0
WHERE EXISTS (SELECT 1 FROM contratos WHERE id = 9001)
  AND NOT EXISTS (SELECT 1 FROM facturas WHERE id = 9102);

INSERT INTO facturas
    (id, contrato_id, periodo, tipo, concepto, fecha_emision, fecha_vencimiento, importe, estado, version)
SELECT 9103, 9001, '2026-09', 'ADICIONAL', 'S02 anulada', '2026-09-01', '2026-09-10', 250000.00, 'ANULADA', 0
WHERE EXISTS (SELECT 1 FROM contratos WHERE id = 9001)
  AND NOT EXISTS (SELECT 1 FROM facturas WHERE id = 9103);

INSERT INTO facturas
    (id, contrato_id, periodo, tipo, concepto, fecha_emision, fecha_vencimiento, importe, estado, version)
SELECT 9104, 9001, '2026-11', 'ADICIONAL', 'S02 rechazos sin escrituras', '2026-11-01', '2026-11-10', 250000.00, 'PENDIENTE', 0
WHERE EXISTS (SELECT 1 FROM contratos WHERE id = 9001)
  AND NOT EXISTS (SELECT 1 FROM facturas WHERE id = 9104);

COMMIT;

-- Debe devolver cuatro facturas; sin pagos antes de la primera ejecucion de Postman.
SELECT f.id, f.estado, f.importe, p.id AS pago_id
FROM facturas f LEFT JOIN pagos p ON p.factura_id = f.id
WHERE f.id IN (9101, 9102, 9103, 9104)
ORDER BY f.id;
