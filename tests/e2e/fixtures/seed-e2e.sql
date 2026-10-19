INSERT INTO venues (code, name, city, address, capacity, active) VALUES
 ('VEN-E2E-01',  'Arena E2E',    'Santa Marta', 'Calle 1 # 2-3', 500, true),
 ('VEN-E2E-CAP1','Sala Aforo 1', 'Santa Marta', 'Calle 4 # 5-6',   1, true);

INSERT INTO events (event_code, name, description, event_date, category, status, minimum_age, venue_id) VALUES
 ('EVT-E2E-DRAFT', 'E2E Borrador',     'Evento en borrador', '2027-06-15', 'MUSIC', 'DRAFT',     0,  (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-PUB',   'E2E Publicado',    'Evento publicado',   '2027-06-20', 'MUSIC', 'PUBLISHED', 0,  (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-TOPUB', 'E2E Por publicar', 'Para PATCH publish', '2027-07-01', 'MUSIC', 'DRAFT',     0,  (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-ADULT', 'E2E Mayores 18',   'Edad minima 18',     '2027-07-15', 'MUSIC', 'PUBLISHED', 18, (SELECT id FROM venues WHERE code='VEN-E2E-01')),
 ('EVT-E2E-CAP1',  'E2E Aforo 1',      'Capacidad 1',        '2027-07-10', 'MUSIC', 'PUBLISHED', 0,  (SELECT id FROM venues WHERE code='VEN-E2E-CAP1'));
