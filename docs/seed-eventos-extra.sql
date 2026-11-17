-- Eventos de DEMOSTRACIÓN adicionales con sedes REALES (solo para tu base local). Complementa a seed-demo.sql.
-- Se puede ejecutar varias veces: no duplica sedes ni eventos (ON CONFLICT DO NOTHING).
-- Los eventos son ficticios; las sedes, direcciones y aforos son reales (aproximados). Fechas de 2027.
-- Para probar «Agotado» baja el aforo, p. ej.:  UPDATE venues SET capacity = 2 WHERE code = 'VEN-CTG-01';

INSERT INTO venues (code, name, city, address, capacity, active) VALUES
 ('VEN-MED-01', 'Teatro Metropolitano José Gutiérrez Gómez', 'Medellín',  'Calle 41 # 57-30',                              1634,  true),
 ('VEN-CAL-01', 'Teatro Municipal Enrique Buenaventura',     'Cali',      'Carrera 5 # 6-64',                              1021,  true),
 ('VEN-CAL-02', 'Coliseo El Pueblo',                         'Cali',      'Cuarto de Legua, Cali',                         12000, true),
 ('VEN-CTG-01', 'Teatro Adolfo Mejía',                       'Cartagena', 'Plaza de la Merced, Calle de la Chichería 38-10', 650,  true),
 ('VEN-CTG-02', 'Centro de Convenciones Cartagena de Indias','Cartagena', 'Calle 24 # 8A-344, Getsemaní',                  1462,  true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO events (event_code, name, description, event_date, category, status, minimum_age, venue_id) VALUES
 ('ACU-2027',  'Concierto Acústico',       'Noche íntima de música en vivo.',      '2027-02-14', 'MUSIC',         'PUBLISHED', 0,
   (SELECT id FROM venues WHERE code = 'VEN-MED-01')),
 ('FUT-2027',  'Clásico de la Costa',      'Torneo amistoso de microfútbol.',      '2027-03-06', 'SPORTS',        'PUBLISHED', 0,
   (SELECT id FROM venues WHERE code = 'VEN-CAL-02')),
 ('EDU-2027',  'Feria de Becas y Estudio', 'Charlas y stands de universidades.',   '2027-04-22', 'EDUCATION',     'PUBLISHED', 15,
   (SELECT id FROM venues WHERE code = 'VEN-CTG-02')),
 ('ART-2027',  'Noche de Teatro',          'Función de gala de teatro colombiano.','2027-05-15', 'CULTURE',       'PUBLISHED', 0,
   (SELECT id FROM venues WHERE code = 'VEN-CAL-01')),
 ('COM-2027',  'Stand-up Caribe',          'Comedia en vivo para mayores de 18.',  '2027-06-05', 'ENTERTAINMENT', 'PUBLISHED', 18,
   (SELECT id FROM venues WHERE code = 'VEN-CTG-01')),
 ('DRAFT-002', 'Gala de Fin de Año (borrador)', 'Solo visible para STAFF/ADMIN.', '2027-12-10', 'CULTURE',       'DRAFT',     0,
   (SELECT id FROM venues WHERE code = 'VEN-CAL-01'))
ON CONFLICT (event_code) DO NOTHING;

INSERT INTO event_artists (event_id, artist_id)
SELECT e.id, a.id
FROM (VALUES ('ACU-2027','Ocean Drive'), ('ACU-2027','Neon Waves'),
             ('COM-2027','Caribbean Sound'),
             ('ART-2027','Solar Beat')) AS v(codigo, artista)
JOIN events e  ON e.event_code = v.codigo
JOIN artists a ON a.stage_name = v.artista
WHERE NOT EXISTS (SELECT 1 FROM event_artists ea WHERE ea.event_id = e.id AND ea.artist_id = a.id);
