-- Datos de prueba para la base E2E (pulsepass_e2e). SOLO ASCII en los comentarios: el script se envia por pipe a psql.
--
-- IMPORTANTE:
--  * NO es una migracion de Flyway: esta fuera de db/migration, asi que Flyway nunca lo ejecuta. Se carga a mano con psql
--    (ver tests/e2e/fixtures/README.md), por eso SI se pueden agregar comentarios sin romper ningun checksum.
--  * Cargar unicamente en la base E2E; nunca en desarrollo, QA ni produccion.
--  * Requiere que las tablas ya existan (arrancar la aplicacion una vez para que Flyway las cree).
--  * No es idempotente: ejecutarlo dos veces falla por codigos unicos duplicados.

-- Dos recintos: uno amplio (capacidad 500) y otro de capacidad 1, para probar el caso de cupo agotado.
INSERT INTO venues (code, name, city, address, capacity, active) VALUES
 ('VEN-E2E-01',  'Arena E2E',    'Santa Marta', 'Calle 1 # 2-3', 500, true),
 ('VEN-E2E-CAP1','Sala Aforo 1', 'Santa Marta', 'Calle 4 # 5-6',   1, true);


-- Cinco eventos, uno por escenario de prueba (el estado y la edad minima cambian segun el caso):
--   EVT-E2E-DRAFT : borrador, invisible para el publico.
--   EVT-E2E-PUB   : publicado, sin edad minima (compra normal).
--   EVT-E2E-TOPUB : borrador que la prueba publica con PATCH /publish.
--   EVT-E2E-ADULT : publicado con edad minima 18.
--   EVT-E2E-CAP1  : publicado en el recinto de capacidad 1 (el segundo comprador encuentra el cupo agotado).
-- El venue_id se resuelve con una subconsulta por codigo, para no depender de ids numericos fijos.
-- Las fechas (2027) deben seguir siendo futuras al ejecutar la prueba; si ya no lo son, actualizalas.
INSERT INTO events (event_code, name, description, event_date, category, status, minimum_age, venue_id) VALUES
 ('EVT-E2E-DRAFT', 'E2E Borrador',     'Evento en borrador', '2027-06-15', 'MUSIC', 'DRAFT',     0,  (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-PUB',   'E2E Publicado',    'Evento publicado',   '2027-06-20', 'MUSIC', 'PUBLISHED', 0,  (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-TOPUB', 'E2E Por publicar', 'Para PATCH publish', '2027-07-01', 'MUSIC', 'DRAFT',     0,  (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-ADULT', 'E2E Mayores 18',   'Edad minima 18',     '2027-07-15', 'MUSIC', 'PUBLISHED', 18, (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-CAP1',  'E2E Aforo 1',      'Capacidad 1',        '2027-07-10', 'MUSIC', 'PUBLISHED', 0,  (SELECT id FROM venues WHERE code='VEN-E2E-CAP1'));
