-- Datos de prueba para S04 - Informes de resolucion (IDs reservados 9201 a 9204)
-- Requiere haber ejecutado previamente scripts/02-datos-contratos.sql (contrato 9001)

INSERT IGNORE INTO incidentes (id, contrato_id, titulo, descripcion, categoria, fecha_alta, prioridad, estado, version)
VALUES
(9201, 9001, 'Perdida en caneria de cocina', 'Goteo constante debajo de la bacha de cocina', 'PLOMERIA', '2026-11-05 09:30:00', 'ALTA', 'EN_PROCESO', 0),
(9202, 9001, 'Corte de luz en dormitorio', 'Salta la termica al encender el aire acondicionado', 'ELECTRICIDAD', '2026-11-06 11:00:00', 'MEDIA', 'EN_PROCESO', 0),
(9203, 9001, 'Calefon no enciende piloto', 'Revision solicitada por inquilino', 'GAS', '2026-11-07 15:15:00', 'ALTA', 'ABIERTO', 0),
(9204, 9001, 'Cerradura puerta balcon', 'Trabada al cerrar desde afuera', 'GENERAL', '2026-11-01 10:00:00', 'BAJA', 'RESUELTO', 0);

INSERT IGNORE INTO historial_estados_incidente (id, incidente_id, estado_anterior, estado_nuevo, fecha_hora)
VALUES
(9201, 9201, NULL, 'ABIERTO', '2026-11-05 09:30:00'),
(9202, 9201, 'ABIERTO', 'EN_PROCESO', '2026-11-05 14:00:00'),
(9203, 9202, NULL, 'ABIERTO', '2026-11-06 11:00:00'),
(9204, 9202, 'ABIERTO', 'EN_PROCESO', '2026-11-06 16:20:00'),
(9205, 9203, NULL, 'ABIERTO', '2026-11-07 15:15:00');
