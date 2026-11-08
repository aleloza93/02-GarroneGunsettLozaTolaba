-- Ejecutar DESPUES del primer arranque: Hibernate crea la tabla contratos.
-- Solo agrega IDs de prueba libres; nunca sobrescribe datos existentes.
-- Reservar 9001 a 9004 para estos casos y verificar su contenido si ya existen.
USE `02-GarroneGunsettLozaTolaba`;

INSERT INTO contratos
    (id, fecha_inicio, duracion_meses, importe_mensual, dia_vencimiento, estado, eliminado)
SELECT 9001, '2026-01-01', 24, 250000.00, 10, 'ACTIVO', FALSE
WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id = 9001);

INSERT INTO contratos
    (id, fecha_inicio, duracion_meses, importe_mensual, dia_vencimiento, estado, eliminado)
SELECT 9002, '2025-01-01', 12, 180000.00, 31, 'FINALIZADO', FALSE
WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id = 9002);

INSERT INTO contratos
    (id, fecha_inicio, duracion_meses, importe_mensual, dia_vencimiento, estado, eliminado)
SELECT 9003, '2026-01-01', 24, 250000.00, 10, 'ACTIVO', TRUE
WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id = 9003);

INSERT INTO contratos
    (id, fecha_inicio, duracion_meses, importe_mensual, dia_vencimiento, estado, eliminado)
SELECT 9004, '2026-01-01', 24, 320000.00, 31, 'ACTIVO', FALSE
WHERE NOT EXISTS (SELECT 1 FROM contratos WHERE id = 9004);

