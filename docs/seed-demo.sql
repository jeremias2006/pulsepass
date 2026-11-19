-- Datos de DEMOSTRACIÓN para desarrollar el frontend. Solo para tu base local.
-- No es una migración de Flyway (está fuera de db/migration): se carga a mano y NO es idempotente.
-- Requiere que la app ya haya arrancado una vez (las tablas deben existir).

-- Sedes REALES con su aforo aproximado. Para probar «Agotado» con pocas compras, baja el aforo a mano, p. ej.:
--   UPDATE venues SET capacity = 3 WHERE code = 'VEN-SMR-01';
INSERT INTO venues (code, name, city, address, capacity, active) VALUES
 ('VEN-SMR-01', 'Estadio Sierra Nevada', 'Santa Marta', 'Sector Bureche, sur de Santa Marta', 16162, true),
 ('VEN-BOG-01', 'Movistar Arena',        'Bogotá',      'Diagonal 61C # 26-36',               14000, true);

-- Las fechas deben seguir siendo FUTURAS al probar la compra; ajústalas si ya pasaron.
INSERT INTO events (event_code, name, description, event_date, category, status, minimum_age, venue_id) VALUES
 ('CMF-2026', 'Caribbean Music Fest 2026', 'Festival de música del Caribe.', '2027-03-20', 'MUSIC',      'PUBLISHED', 18,
   (SELECT id FROM venues WHERE code = 'VEN-SMR-01')),
 ('TECH-2027', 'Tech Summit 2027',         'Conferencia de tecnología.',      '2027-04-10', 'TECHNOLOGY', 'PUBLISHED', 0,
   (SELECT id FROM venues WHERE code = 'VEN-BOG-01')),
 ('DRAFT-001', 'Evento en borrador',       'Solo visible para STAFF/ADMIN.',  '2027-05-01', 'CULTURE',    'DRAFT',     0,
   (SELECT id FROM venues WHERE code = 'VEN-BOG-01'));

INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id FROM events e, artists a
WHERE e.event_code = 'CMF-2026' AND a.stage_name IN ('Solar Beat', 'Neon Waves');
