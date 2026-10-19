# PulsePass — Matriz de trazabilidad E2E

Relaciona cada requisito del PRD con los casos y archivos que lo respaldan. Los resultados provienen de `registro-ejecucion-manual.md`.

Estados: **Cumple** (hay evidencia documental o resultado PASS en la bitácora) y **Parcial** (depende de casos aún sin ejecutar, sección 6.1 de la bitácora).

---

## 1. Requisitos funcionales (FR-E2E)

| Requisito | Descripción | Evidencia | Estado |
|---|---|---|---|
| FR-E2E-001 | Colección v2.1 importable con las 22 rutas por módulo | `postman/PulsePass-Manual-E2E.postman_collection.json` (carpetas 00 a 09); bitácora §7 | Cumple |
| FR-E2E-002 | Entorno parametrizable | `postman/PulsePass-local.postman_environment.json` | Cumple |
| FR-E2E-003 | Autenticación manual | `E2EUSR01-A/B`, `E2EAUTH01-A/B/STAFF/ADMIN`; README §4 | Cumple |
| FR-E2E-004 | Variables de token por actor | `userAToken`, `userBToken`, `userCToken`, `staffToken`, `adminToken` | Cumple |
| FR-E2E-005 | Comprobación manual del JSON esperado | Descripción de cada solicitud; columnas esperado y observado de la bitácora | Cumple |
| FR-E2E-006 | Encadenamiento manual de códigos e IDs | README §4 (tabla de copia); descripción de la carpeta 09 | Cumple |
| FR-E2E-007 | Matriz de autorización: público, 401, 403 y propiedad | `TC-SEC001` a `TC-SEC016` y `TC-AUTH007` a `TC-AUTH015` (bitácora §2 a §4). `TC-SEC005` y `TC-SEC017` sin ejecutar | Parcial |
| FR-E2E-008 | Integridad funcional tras cada transición | Filas "Integridad T1", "Reconsulta T1", "Reconsulta T2", "Visible tras publicar", "TC-AUTH014 integridad" | Cumple |
| FR-E2E-009 | Revisión de `ErrorResponse` | Observación de cada error en la bitácora; fila "ErrorResponse" de §6 | Cumple |
| FR-E2E-010 | Datos controlados y restauración manual | `fixtures/seed-e2e.sql`, `fixtures/README.md`; README §8 | Cumple |
| FR-E2E-011 | Instrucciones paso a paso | README §4 y §7; descripciones por solicitud; `checklist-e2e-manual.md` | Cumple |
| FR-E2E-012 | Bitácora manual | `registro-ejecucion-manual.md` | Cumple |
| FR-E2E-013 | Confidencialidad | Secretos vacíos en el ambiente; `tests/e2e/.gitignore`; README §9 | Cumple |
| FR-E2E-014 | Registro de regresiones | Bitácora §9 (sin defectos; hallazgos H1 y H2) | Cumple |
| FR-E2E-015 | Bloqueos como BLOCKED | README §5; bitácora sin casos BLOCKED | Cumple |
| FR-E2E-016 | Repetibilidad | README §8 (restaurar base, recargar seed, cambiar `runId`) | Cumple |
| FR-E2E-017 | Contratos heredados | Cuerpos alineados con `RegisterUserRequest`, `LoginRequest`, `CreateEventRequest` y `PurchaseTicketRequest`; valores de `EventCategory` y `TicketType` | Cumple |

---

## 2. Rutas, casos y resultado

| # | Método y ruta | Caso nominal | Casos negativos y de seguridad | Resultado |
|---|---|---|---|---|
| 1 | `GET /api/venues/{code}` | E2EVEN01 | E2EVEN01-neg (404) | PASS |
| 2 | `GET /api/venues/active` | E2EVEN02 / TC-BIZ001 | TC-AUTH015 (token inválido en ruta pública) | PASS |
| 3 | `POST /api/events` | E2EEVT01 / TC-BIZ003 | TC-SEC001a/b/c, TC-AUTH014, TC-BIZ015-dup/cat/venue/name | PASS |
| 4 | `GET /api/events/{eventCode}` | E2EEVT02 | TC-SEC004, TC-SEC015, borrador con STAFF | PASS |
| 5 | `GET /api/events/published` | E2EEVT03 | Los borradores no aparecen | PASS |
| 6 | `PATCH /api/events/{eventCode}/publish` | E2EEVT04 / TC-BIZ004 | TC-SEC002a/b, publicar otra vez (409) | PASS |
| 7 | `POST /api/events/{eventCode}/artists/{artistId}` | E2EEVT05 / TC-BIZ005 | TC-SEC003, asociación repetida (409), artista 999999 (404) | PASS |
| 8 | `GET /api/events/by-artist` | E2EEVT06 | TC-SEC005-pre y TC-SEC005a/b (sin ejecutar) | PASS nominal; seguridad pendiente |
| 9 | `GET /api/artists/{id}` | E2EART01 | E2EART01-neg (404) | PASS |
| 10 | `GET /api/artists/by-stage-name` | E2EART02 | — | PASS |
| 11 | `GET /api/artists/active` | E2EART03 / TC-BIZ002 | — | PASS |
| 12 | `POST /api/users` | E2EUSR01-A/B / TC-AUTH001 / TC-BIZ006 | TC-SEC016; duplicado (409); email inválido, firstName ausente, password corta y JSON inválido (400) | PASS |
| 13 | `GET /api/users/by-email` | E2EUSR02 | TC-SEC006 | PASS |
| 14 | `GET /api/users/by-username` | E2EUSR03 | TC-SEC007 | PASS |
| 15 | `POST /api/tickets` | E2ETKT01 / TC-BIZ007 | TC-SEC008; ADMIN y STAFF (403); TC-BIZ014 (borrador, evento inexistente, edad, aforo, tipo y email inválidos) | PASS |
| 16 | `GET /api/tickets/{ticketCode}` | E2ETKT02 / TC-BIZ008 | TC-SEC009 | PASS |
| 17 | `GET /api/tickets/by-user` | E2ETKT03 / TC-BIZ008 | TC-SEC010 | PASS |
| 18 | `GET /api/events/{eventCode}/tickets/paid` | E2ETKT04 / TC-BIZ009 | TC-SEC013 | PASS |
| 19 | `PATCH /api/tickets/{ticketCode}/cancel` | E2ETKT05 / TC-BIZ010 | TC-BIZ011 (409), TC-SEC011, STAFF cancela (403), ticket inexistente (404) | PASS |
| 20 | `PATCH /api/tickets/{ticketCode}/use` | E2ETKT06 / TC-BIZ012 / TC-SEC014 | TC-SEC014-pre, TC-SEC012, TC-BIZ013 (409), ticket cancelado (409) | PASS |
| 21 | `POST /api/auth/login` | E2EAUTH01-A/B/STAFF/ADMIN / TC-AUTH002 / TC-AUTH003 | TC-AUTH004, TC-AUTH005 | PASS |
| 22 | `GET /api/auth/me` | E2EAUTH02-A/B/STAFF/ADMIN / TC-AUTH006 | TC-AUTH007 a TC-AUTH013, TC-SEC016 | PASS |

---

## 3. Casos sin ejecutar

| Caso | Qué comprueba | Estado | Requisito previo |
|---|---|---|---|
| TC-SEC005-pre | Asociar un artista al borrador (200, sigue en DRAFT) | NOT RUN | Token de ADMIN |
| TC-SEC005a | `by-artist` anónimo no incluye borradores | NOT RUN | `TC-SEC005-pre` ejecutado |
| TC-SEC005b | `by-artist` con ADMIN sí incluye el borrador | NOT RUN | `TC-SEC005-pre` ejecutado |
| TC-SEC017a | Usuario con STAFF temporal accede al borrador (200) | NOT RUN | Rol asignado por SQL y login |
| TC-SEC017b | Con el mismo token y el rol revocado, responde 404 | NOT RUN | Rol revocado por SQL |

---

## 4. Estado global

| Indicador | Valor |
|---|---|
| Rutas con caso nominal en PASS | 22 de 22 |
| Casos en PASS | 103 |
| FAIL / BLOCKED | 0 / 0 |
| Sin ejecutar | 5 |
| Requisitos FR-E2E cumplidos | 16 de 17 |

Al ejecutar los 5 casos pendientes hay que actualizar esta matriz: FR-E2E-007 pasa a Cumple, la ruta 8 a PASS completo y el total de requisitos a 17 de 17.