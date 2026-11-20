-- Datos de prueba para S03 - Incidentes (IDs reservados 9101 a 9199)
-- Requiere haber ejecutado previamente scripts/02-datos-contratos.sql (contrato 9001)
-- No pisa los datos de S04 (incidentes 9201 a 9204).

INSERT IGNORE INTO incidentes (id, contrato_id, titulo, descripcion, categoria, fecha_alta, prioridad, estado, version)
VALUES
(9101, 9001, 'Humedad en pared del living', 'Mancha de humedad que crece desde el zocalo', 'GENERAL', '2026-11-10 10:00:00', 'MEDIA', 'ABIERTO', 0),
(9102, 9001, 'Toma corriente sin tension', 'El toma de la cocina no funciona', 'ELECTRICIDAD', '2026-11-11 09:15:00', 'ALTA', 'EN_PROCESO', 0),
(9103, 9001, 'Canilla de bano gotea', 'Goteo constante en lavatorio', 'PLOMERIA', '2026-11-03 08:30:00', 'BAJA', 'RESUELTO', 0),
(9104, 9001, 'Olor a gas en cocina', 'Reclamo duplicado, se atendio en otro incidente', 'GAS', '2026-11-04 18:00:00', 'ALTA', 'CANCELADO', 0);

INSERT IGNORE INTO historial_estados_incidente (id, incidente_id, estado_anterior, estado_nuevo, fecha_hora)
VALUES
(9101, 9101, NULL,         'ABIERTO',    '2026-11-10 10:00:00'),
(9102, 9102, NULL,         'ABIERTO',    '2026-11-11 09:15:00'),
(9103, 9102, 'ABIERTO',    'EN_PROCESO', '2026-11-11 12:00:00'),
(9104, 9103, NULL,         'ABIERTO',    '2026-11-03 08:30:00'),
(9105, 9103, 'ABIERTO',    'EN_PROCESO', '2026-11-03 11:00:00'),
(9106, 9103, 'EN_PROCESO', 'RESUELTO',   '2026-11-04 16:45:00'),
(9107, 9104, NULL,         'ABIERTO',    '2026-11-04 18:00:00'),
(9108, 9104, 'ABIERTO',    'CANCELADO',  '2026-11-04 18:30:00');

-- Informe de la resolucion de 9103 (misma fecha y hora que su transicion a RESUELTO)
INSERT IGNORE INTO informes_resolucion (id, incidente_id, trabajo_realizado, fecha_resolucion, costo, observaciones, responsable)
VALUES
(9101, 9103, 'Se cambio el cuerito y se ajusto la canilla', '2026-11-04 16:45:00', 4500.00, NULL, 'Laura Mendez');
