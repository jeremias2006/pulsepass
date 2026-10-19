# PulsePass — Registro de ejecución manual E2E

- Base de datos: `pulsepass_e2e`
- runId: `r01`
- Fecha de inicio: 2026-10-19
- Reglas: PASS solo si HTTP, JSON y efecto de negocio coinciden. Si falta un prerrequisito, BLOCKED. Un 500 es FAIL automático.
- No pegar JWT, contraseñas ni datos personales reales en este archivo.

**Resultados posibles:** PASS · FAIL · BLOCKED · NOT RUN

---

## 0. Conectividad

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| 0.1 | 2026-10-19 03:49 | Anónimo | GET /api/venues/active | 200 | 200 | VEN-E2E-01 (cap. 500) y VEN-E2E-CAP1 | PASS |  |

## 1. Flujo A — Descubrimiento público

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| E2EVEN02 / TC-BIZ001 | 2026-10-19 03:49 | Anónimo | GET /api/venues/active | 200 | 200 | Cada venue trae id, code, name, city, capacity, active; dos venues del fixture | PASS |  |
| E2EVEN01 | 2026-10-19 04:16 | Anónimo | GET /api/venues/{{venueActiveCode}} | 200 | 200 | id, code VEN-E2E-01, name "Arena E2E", city "Santa Marta", capacity 500, active true | PASS | |
| E2EVEN01-neg | 2026-10-19 04:16 | Anónimo | GET /api/venues/NO-EXISTE | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Venue not found: NO-EXISTE" | PASS | |
| E2EART03 / TC-BIZ002 | 2026-10-19 04:16 | Anónimo | GET /api/artists/active | 200 | 200 | 5 artistas activos ordenados por stageName: Caribbean Sound, Digital Pulse, Neon Waves, Ocean Drive, Solar Beat | PASS | Copiado artistId=1 y artistStageName="Solar Beat" |
| E2EART01 | 2026-10-19 04:16 | Anónimo | GET /api/artists/{{artistId}} | 200 | 200 | id 1, stageName "Solar Beat", active true | PASS | |
| E2EART01-neg | 2026-10-19 04:17 | Anónimo | GET /api/artists/999999 | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Artist not found: 999999" | PASS | |
| E2EART02 | 2026-10-19 04:17 | Anónimo | GET /api/artists/by-stage-name | 200 | 200 | Mismo artista id 1 ("Solar Beat"); la búsqueda ignora mayúsculas | PASS | Enviado con stageName=Solar%20Beat |
| E2EEVT03 / TC-SEC005 | 2026-10-19 04:17 | Anónimo | GET /api/events/published | 200 | 200 | 3 eventos PUBLISHED por fecha: EVT-E2E-PUB (2027-06-20), EVT-E2E-CAP1 (2027-07-10), EVT-E2E-ADULT (2027-07-15); no aparecen EVT-E2E-DRAFT ni EVT-E2E-TOPUB | PASS | |
| E2EEVT02 | 2026-10-19 04:17 | Anónimo | GET /api/events/{{publishedEventCode}} | 200 | 200 | EVT-E2E-PUB, status PUBLISHED, minimumAge 0, venueCode VEN-E2E-01, artists [] | PASS | |
| TC-SEC004 | 2026-10-19 04:18 | Anónimo | GET /api/events/{{draftEventCode}} | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Event not found: EVT-E2E-DRAFT" | PASS | Mismo formato que un código inexistente: no revela que el borrador existe |
| E2EEVT06 | 2026-10-19 04:24 | Anónimo | GET /api/events/by-artist | 200 | 200 | [EVT-r01] solo PUBLISHED (summary: id, eventCode, name, category, status, eventDate, venueName) | PASS | Ejecutado después de publicar EVT-r01 (Flujo C). El fixture no asocia artistas a borradores, así que la exclusión de DRAFT no se pudo observar con datos; la cubre el filtro de EventServiceImpl.findByArtist |

## 2. Flujo B — Registro y JWT

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| E2EUSR01-A / TC-AUTH001 / TC-BIZ006 | 2026-10-19 ~03:12 | USER_A | POST /api/users | 201 | 201 | id 3, usera_r01, active true, firstName Andrea, lastName Pruebas, phone null, city null, birthDate 1998-05-15; sin password ni hash | PASS | 201 y sin password ni hash en la respuesta |
| E2EUSR01-B | 2026-10-19 ~03:12 | USER_B | POST /api/users | 201 | 201 | id 4, userb_r01, active true; sin password ni hash | PASS | 201 |
| Registro duplicado | 2026-10-19 04:01 | USER_A (mismos datos) | POST /api/users | 409 | 409 | ErrorResponse con 5 campos, status 409, error "Conflict" | PASS | message: "Username already exists: usera_r01". El mensaje revela qué username existe; es el comportamiento esperado de un 409 de registro |
| Email mal formado | 2026-10-19 04:02 | Anónimo | POST /api/users | 400 | 400 | status 400, message "Validation failed", details.email "Email format is invalid" | PASS |  |
| Falta firstName | 2026-10-19 04:03 | Anónimo | POST /api/users | 400 | 400 | status 400, details.firstName "First name is required" (y details.email, porque el body seguía con email "abc") | PASS | reenviado sin la línea firstName (campo ausente) y con email válido: 400, details.firstName "First name is required" |
| Password corta | 2026-10-19 04:03 | Anónimo | POST /api/users | 400 | 400 | status 400, details.password "Password must be between 12 and 72 characters" (también firstName y email, por arrastre del body anterior) | PASS | El rango válido es de 12 a 72 caracteres |
| JSON inválido | 2026-10-19 04:04 | Anónimo | POST /api/users (sin llave de cierre) | 400 | 400 | status 400, message "Malformed or invalid JSON request", details.body "Check JSON syntax, dates and enum values" | PASS | Sin stack trace ni nombres de clase |
| TC-SEC016 (registro) | 2026-10-19 04:08 | Anónimo | POST /api/users con role=ADMIN | 201 | 201 | id 5, usere_r01, active true, firstName, lastName, phone null, city null, birthDate | PASS | Sin password ni hash en la respuesta; el campo role se ignoró |
| TC-SEC016 (login) | 2026-10-19 ~04:12 | usere_r01 | POST /api/auth/login | 200 | 200 | accessToken, Bearer, expiresIn 900 | PASS | Token no guardado en ninguna variable |
| TC-SEC016 (/me) | 2026-10-19 04:13 | usere_r01 | GET /api/auth/me | 200 | 200 | id 5, usere_r01, roles ["USER"] | PASS | El rol inyectado no se aplicó: sin escalada de privilegios |
| E2EAUTH01-A / TC-AUTH002 | 2026-10-19 03:13 | USER_A | POST /api/auth/login (username) | 200 | 200 | accessToken, tokenType Bearer, expiresIn 900 | PASS |  |
| E2EAUTH01-B | 2026-10-19 ~03:17 | USER_B | POST /api/auth/login | 200 | 200 | Token copiado a userBToken | PASS |  |
| E2EAUTH01-STAFF | 2026-10-19 ~03:17 | STAFF | POST /api/auth/login | 200 | 200 | Token copiado a staffToken | PASS |  |
| E2EAUTH01-ADMIN | 2026-10-19 ~03:17 | ADMIN | POST /api/auth/login | 200 | 200 | Token copiado a adminToken | PASS |  |
| TC-AUTH003 | 2026-10-19 ~03:23 | USER_A | POST /api/auth/login (email) | 200 | 200 | accessToken, Bearer, expiresIn 900 | PASS |  |
| TC-AUTH004 | 2026-10-19 03:23 | USER_A | POST /api/auth/login (password mala) | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | message: "Autenticación requerida o credenciales inválidas" |
| TC-AUTH005 | 2026-10-19 03:24 | Anónimo | POST /api/auth/login (identifier inexistente) | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | Mismo message y mismo tamaño (618 B) que TC-AUTH004: sin enumeración de usuarios |
| E2EAUTH02-A / TC-AUTH006 | 2026-10-19 03:16 | USER_A | GET /api/auth/me | 200 | 200 | id 3, usera_r01, roles ["USER"], sin credenciales | PASS |  |
| E2EAUTH02-B | 2026-10-19 ~03:18 | USER_B | GET /api/auth/me | 200 | 200 | Identidad de B | PASS |  |
| E2EAUTH02-STAFF | 2026-10-19 ~03:18 | STAFF | GET /api/auth/me | 200 | 200 | Identidad de laura | PASS | roles ["STAFF"] |
| E2EAUTH02-ADMIN | 2026-10-19 03:53 | ADMIN | GET /api/auth/me | 200 | 200 | id 1, admin, admin@pulsepass.local, roles ["ADMIN"] | PASS |  |
| TC-AUTH007 | 2026-10-19 03:27 | Anónimo | GET /api/auth/me (No Auth) | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS |  |
| TC-AUTH008 | 2026-10-19 03:29 | Bearer abc.def.ghi | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS |  |
| TC-AUTH009 | 2026-10-19 03:32 | Token alterado (último carácter) | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | el token original sin alterar da 200 en /me (04:17, token renovado) |
| TC-AUTH010 | 2026-10-19 03:42 | USER_B con token vencido (~25 min) | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | El mismo token había dado 200 a las 03:17 |
| TC-AUTH011 | 2026-10-19 03:42 | Token firmado con clave inventada | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | Payload válido y vigente (sub 3, iss pulsepass-api); solo la firma era falsa. Clave inventada, no la real |
| TC-AUTH012 | 2026-10-19 03:45 | Token por query string, sin header | GET /api/auth/me?access_token=... | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | reenviado 04:18 con userAToken vigente (renovado 04:16); sigue en 401: solo se acepta el header Authorization |
| TC-AUTH013 | 2026-10-19 04:18 | Usuario inactivo | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401; mensaje genérico, sin detalle de la causa | PASS | Se usó usere_r01 (login 200 antes de desactivarlo) con UPDATE users SET active = false WHERE username = 'usere_r01'. No se desactivó a USER_A para no bloquear los flujos D y de reglas |
| TC-AUTH014 | 2026-10-19 03:47 | USER_A | POST /api/events | 403 | 403 | ErrorResponse con 5 campos, status 403, message "No tiene permisos para realizar esta operación" | PASS |  |
| TC-AUTH014 integridad | 2026-10-19 03:52 | ADMIN | GET /api/events/{{newEventCode}} | 404 | 404 | "Event not found: EVT-r01" | PASS | reenviado 04:18 con adminToken vigente; sigue en 404 |
| TC-AUTH015 | 2026-10-19 03:50 | Bearer abc.def.ghi en ruta pública | GET /api/venues/active | anotar lo observado | 200 | Devuelve los dos venues | PASS | La ruta pública ignora el token inválido; no lo rechaza ni lo trata como usuario. Sin 500 ni datos internos |

## 3. Flujo C — Catálogo (ADMIN)

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| TC-SEC001a | 2026-10-19 04:18 | Anónimo | POST /api/events | 401 | 401 | ErrorResponse con 5 campos, status 401, message "Autenticación requerida o credenciales inválidas"; header WWW-Authenticate: Bearer | PASS | |
| TC-SEC001b | 2026-10-19 04:19 | USER_A | POST /api/events | 403 | 403 | ErrorResponse con 5 campos, status 403, message "No tiene permisos para realizar esta operación" | PASS | Repite TC-AUTH014 (03:47) con token renovado; mismo resultado |
| TC-SEC001c | 2026-10-19 04:19 | STAFF | POST /api/events | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| TC-SEC002a | 2026-10-19 04:19 | USER_A | PATCH /api/events/{{eventToPublishCode}}/publish | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | Rechazo por regla de URL (ADMIN), antes de tocar el evento |
| TC-SEC002b | 2026-10-19 04:19 | STAFF | PATCH /api/events/{{eventToPublishCode}}/publish | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| TC-SEC003 | 2026-10-19 04:20 | USER_A | POST /api/events/{{publishedEventCode}}/artists/{{artistId}} | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | Evento EVT-E2E-PUB con artistId=1 |
| Integridad tras 403 | 2026-10-19 04:20 | ADMIN | GET /api/events/{{eventToPublishCode}} y {{newEventCode}} | 200 DRAFT / 404 | 200 / 404 | EVT-E2E-TOPUB: 200, status DRAFT, sin cambios. EVT-r01: 404 "Event not found: EVT-r01" | PASS | Ningún 403 anterior modificó ni creó datos |
| E2EEVT01 / TC-BIZ003 | 2026-10-19 04:20 | ADMIN | POST /api/events | 201 | 201 | EVT-r01, name "PulsePass E2E r01", category MUSIC, status DRAFT, eventDate 2027-08-20, minimumAge 0, venueCode VEN-E2E-01, artists [] | PASS | El request no controla el estado: siempre nace en DRAFT |
| Código duplicado | 2026-10-19 04:21 | ADMIN | POST /api/events | 409 | 409 | ErrorResponse con 5 campos, status 409, error "Conflict", message "Event code already exists: EVT-r01" | PASS | |
| Categoría inválida | 2026-10-19 04:21 | ADMIN | POST /api/events | 400 | 400 | status 400, message "Malformed or invalid JSON request", details.body "Check JSON syntax, dates and enum values" | PASS | category "FOO" no es un valor del enum; sin nombres de clase ni stack trace |
| Venue inexistente | 2026-10-19 04:21 | ADMIN | POST /api/events | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Venue not found: NO-EXISTE" | PASS | |
| Falta name | 2026-10-19 04:21 | ADMIN | POST /api/events | 400 | 400 | status 400, message "Validation failed", details.name "Name is required" | PASS | |
| TC-SEC004 (borrador anónimo) | 2026-10-19 04:22 | Anónimo | GET /api/events/{{newEventCode}} | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Event not found: EVT-r01" | PASS | |
| TC-SEC015 | 2026-10-19 04:22 | ADMIN | GET /api/events/{{newEventCode}} | 200 | 200 | EVT-r01, status DRAFT | PASS | |
| Borrador con STAFF | 2026-10-19 04:22 | STAFF | GET /api/events/{{newEventCode}} | 200 | 200 | EVT-r01, status DRAFT | PASS | STAFF también ve borradores |
| E2EEVT05 / TC-BIZ005 | 2026-10-19 04:23 | ADMIN | POST /api/events/{{newEventCode}}/artists/{{artistId}} | 200 | 200 | EVT-r01 con artists [{id 1, "Solar Beat", active true}]; sigue en DRAFT | PASS | |
| Asociación repetida | 2026-10-19 04:23 | ADMIN | POST (misma asociación) | anotar | 409 | ErrorResponse con 5 campos, status 409, message "Artist 1 is already assigned to event EVT-r01" | PASS | La repetida da 409, no 200. El evento sigue con un solo artista |
| Artista 999999 | 2026-10-19 04:23 | ADMIN | POST .../artists/999999 | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Artist not found: 999999" | PASS | |
| E2EEVT04 / TC-BIZ004 | 2026-10-19 04:23 | ADMIN | PATCH /api/events/{{newEventCode}}/publish | 200 | 200 | EVT-r01, status PUBLISHED, artists con Solar Beat | PASS | |
| Publicar otra vez | 2026-10-19 04:24 | ADMIN | PATCH .../publish | 409 | 409 | ErrorResponse con 5 campos, status 409, message "Only DRAFT events can be published. Current status: PUBLISHED" | PASS | Estado sin cambios |
| Visible tras publicar | 2026-10-19 04:24 | Anónimo | GET /api/events/{{newEventCode}} y /published y /by-artist | 200 | 200 | GET /api/events/EVT-r01: PUBLISHED. /published: incluye EVT-r01. /by-artist?stageName=Solar Beat: incluye EVT-r01 | PASS | Los tres como anónimo |

## 4. Flujo D — Tickets

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| E2ETKT01 / TC-BIZ007 | 2026-10-19 04:24 | USER_A | POST /api/tickets (GENERAL) | 201 | 201 | ticketCode TKT-<8 hex>, type GENERAL, price 50.00, status PAID, userEmail de A, eventCode EVT-E2E-PUB | PASS | Copiado a ticketCancelableCode (T1) |
| E2ETKT02 | 2026-10-19 04:25 | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | 200 | T1: PAID, mismo ticketCode, dueño A | PASS | |
| E2ETKT03 | 2026-10-19 04:25 | USER_A | GET /api/tickets/by-user?email=A | 200 | 200 | Lista con 1 ticket (T1) | PASS | |
| E2EUSR02 | 2026-10-19 04:25 | USER_A | GET /api/users/by-email?email=A | 200 | 200 | id 3, usera_r01, usera.r01@example.test, active true, firstName Andrea, birthDate 1998-05-15; sin password ni hash | PASS | |
| E2EUSR03 | 2026-10-19 04:26 | USER_A | GET /api/users/by-username?username=A | 200 | 200 | Mismo usuario A | PASS | |
| TC-SEC006 | 2026-10-19 04:26 | USER_B | GET /api/users/by-email?email=A | 403 | 403 | ErrorResponse con 5 campos, status 403; el cuerpo no incluye datos de A | PASS | |
| TC-SEC007 | 2026-10-19 04:26 | USER_B | GET /api/users/by-username?username=A | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| TC-SEC009 | 2026-10-19 04:26 | USER_B | GET /api/tickets/{{ticketCancelableCode}} | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| TC-SEC010 | 2026-10-19 04:27 | USER_B | GET /api/tickets/by-user?email=A | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| TC-SEC011 | 2026-10-19 04:27 | USER_B | PATCH .../cancel (ticket de A) | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | Autorización evaluada antes de mutar: T1 sigue PAID |
| TC-SEC012 | 2026-10-19 04:27 | USER_B | PATCH .../use (ticket de A) | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | Rechazo por regla de URL (STAFF/ADMIN) |
| Integridad T1 | 2026-10-19 04:28 | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | 200 | T1: PAID, dueño A | PASS | Los 403 de B no cambiaron el ticket |
| TC-SEC008 / AC-05 | 2026-10-19 04:28 | USER_A | POST /api/tickets con userEmail de B | 403 | 403 | ErrorResponse con 5 campos, status 403; B lista sus tickets (by-user): 200, lista vacía | PASS | No se creó ticket para B |
| ADMIN compra | 2026-10-19 04:28 | ADMIN | POST /api/tickets | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | Solo ROLE_USER compra (regla de URL) |
| STAFF compra | 2026-10-19 04:28 | STAFF | POST /api/tickets | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| E2ETKT05 / TC-BIZ010 | 2026-10-19 04:29 | USER_A | PATCH .../cancel (T1) | 200 | 200 | T1: status CANCELLED | PASS | |
| Reconsulta T1 | 2026-10-19 04:29 | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | 200 | T1: CANCELLED | PASS | |
| TC-BIZ011 | 2026-10-19 04:29 | USER_A | PATCH .../cancel (T1 otra vez) | 409 | 409 | ErrorResponse con 5 campos, status 409, message "Only PAID tickets can be cancelled. Current status: CANCELLED" | PASS | T1 sigue CANCELLED |
| STAFF cancela | 2026-10-19 04:30 | STAFF | PATCH .../cancel | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | Probado sobre T1 (ya CANCELLED): da 403 y no 409, porque la autorización se evalúa antes del estado |
| Cancelar inexistente | 2026-10-19 04:30 | ADMIN | PATCH /api/tickets/NO-EXISTE/cancel | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Ticket not found: NO-EXISTE" | PASS | ADMIN pasa la autorización y falla en la búsqueda |
| Compra T2 (VIP) | 2026-10-19 04:30 | USER_A | POST /api/tickets | 201 | 201 | ticketCode TKT-<8 hex>, type VIP, price 100.00, status PAID | PASS | Copiado a ticketUsableCode (T2) |
| E2ETKT04 / TC-BIZ009 | 2026-10-19 04:30 | STAFF | GET /api/events/{{publishedEventCode}}/tickets/paid | 200 | 200 | 1 ticket: T2 (PAID, VIP). T1 no aparece (CANCELLED) | PASS | |
| TC-SEC013 | 2026-10-19 04:31 | USER_A | GET .../tickets/paid | 403 | 403 | ErrorResponse con 5 campos, status 403 | PASS | |
| USER usa ticket | 2026-10-19 04:31 | USER_A | PATCH .../use (T2) | 403 | 403 | ErrorResponse con 5 campos, status 403; T2 sigue PAID (GET del dueño) | PASS | |
| E2ETKT06 / TC-BIZ012 | 2026-10-19 04:31 | STAFF | PATCH .../use (T2) | 200 | 200 | T2: status USED | PASS | |
| Reconsulta T2 | 2026-10-19 04:31 | USER_A | GET /api/tickets/{{ticketUsableCode}} | 200 | 200 | T2: USED | PASS | |
| TC-BIZ013 | 2026-10-19 04:32 | STAFF | PATCH .../use (T2 otra vez) | 409 | 409 | ErrorResponse con 5 campos, status 409, message "Only PAID tickets can be used. Current status: USED" | PASS | |
| Usar ticket cancelado | 2026-10-19 04:32 | STAFF | PATCH .../use (T1) | 409 | 409 | ErrorResponse con 5 campos, status 409, message "Cancelled ticket cannot be used: <código de T1>" | PASS | |

## 5. Reglas de negocio (TC-BIZ014)

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| Evento en borrador | 2026-10-19 04:32 | USER_A | POST /api/tickets | 409 | 409 | ErrorResponse con 5 campos, status 409, message "Tickets can only be purchased for PUBLISHED events. Current status: DRAFT" | PASS | EVT-E2E-DRAFT; el by-user de A no muestra ticket nuevo |
| Evento inexistente | 2026-10-19 04:33 | USER_A | POST /api/tickets | 404 | 404 | ErrorResponse con 5 campos, status 404, message "Event not found: NO-EXISTE" | PASS | |
| Edad insuficiente | 2026-10-19 04:33 | USER_C | POST /api/tickets (EVT-E2E-ADULT) | 409 | 409 | ErrorResponse con 5 campos, status 409, message "User does not meet minimum age of 18 for event EVT-E2E-ADULT" | PASS | Registro de userc_r01 (201) y login (200). Edad calculada a la fecha del evento (2027-07-15): 12 años |
| Aforo: A compra CAP1 | 2026-10-19 04:33 | USER_A | POST /api/tickets (EVT-E2E-CAP1) | 201 | 201 | Ticket GENERAL, 50.00, PAID en EVT-E2E-CAP1. GET del evento con ADMIN: status SOLD_OUT | PASS | Ver hallazgo H1 (sección 9) |
| Aforo: B compra CAP1 | 2026-10-19 04:33 | USER_B | POST /api/tickets (EVT-E2E-CAP1) | 409 | 409 | ErrorResponse con 5 campos, status 409, message "Tickets can only be purchased for PUBLISHED events. Current status: SOLD_OUT" | PASS | El rechazo sale por la regla de estado, no por la de aforo, porque el evento ya está SOLD_OUT. La regla de aforo solo se ve con compras simultáneas |
| Tipo inválido | 2026-10-19 04:34 | USER_A | POST /api/tickets | 400 | 400 | status 400, message "Malformed or invalid JSON request", details.body "Check JSON syntax, dates and enum values" | PASS | type "FOO" |
| Email mal formado | 2026-10-19 04:34 | USER_A | POST /api/tickets | 400 | 400 | status 400, message "Validation failed", details.userEmail "Email format is invalid" | PASS | La validación del body corre antes que la autorización: da 400 y no 403 |

## 6. Regresión de errores

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| 405 | 2026-10-19 04:34 | ADMIN | DELETE /api/events/EVT-E2E-PUB | 405 | 405 | ErrorResponse con 5 campos, status 405, error "Method Not Allowed", message "HTTP method not supported for this resource"; header Allow presente | PASS | |
| 415 | 2026-10-19 04:35 | USER_A | POST con Content-Type text/plain | 415 | 415 | ErrorResponse con 5 campos, status 415, message "Unsupported media type. Use application/json" | PASS | POST /api/users con text/plain; ruta pública, el token no influye |
| ErrorResponse | 2026-10-19 04:35 | — | Revisar en cada error | — | — | 5 campos (timestamp, status, error, message, details); status = HTTP; sin stack trace, hash ni token | PASS | Revisado en 400, 401, 403, 404, 405, 409 y 415 |
| Ningún 500 | 2026-10-19 04:35 | — | Revisar en toda la ejecución | — | — | Ningún 500 en los 103 casos | PASS |  |

---

## 7. Cobertura de las 22 rutas

Marcar cuando la ruta tenga al menos un caso nominal en PASS.

| # | Ruta | Caso | Cubierta |
|---|---|---|---|
| 1 | GET /api/venues/{code} | E2EVEN01 | [x] |
| 2 | GET /api/venues/active | E2EVEN02 | [x] |
| 3 | POST /api/events | E2EEVT01 | [x] |
| 4 | GET /api/events/{eventCode} | E2EEVT02 | [x] |
| 5 | GET /api/events/published | E2EEVT03 | [x] |
| 6 | PATCH /api/events/{eventCode}/publish | E2EEVT04 | [x] |
| 7 | POST /api/events/{eventCode}/artists/{artistId} | E2EEVT05 | [x] |
| 8 | GET /api/events/by-artist | E2EEVT06 | [x] |
| 9 | GET /api/artists/{id} | E2EART01 | [x] |
| 10 | GET /api/artists/by-stage-name | E2EART02 | [x] |
| 11 | GET /api/artists/active | E2EART03 | [x] |
| 12 | POST /api/users | E2EUSR01 | [x] |
| 13 | GET /api/users/by-email | E2EUSR02 | [x] |
| 14 | GET /api/users/by-username | E2EUSR03 | [x] |
| 15 | POST /api/tickets | E2ETKT01 | [x] |
| 16 | GET /api/tickets/{ticketCode} | E2ETKT02 | [x] |
| 17 | GET /api/tickets/by-user | E2ETKT03 | [x] |
| 18 | GET /api/events/{eventCode}/tickets/paid | E2ETKT04 | [x] |
| 19 | PATCH /api/tickets/{ticketCode}/cancel | E2ETKT05 | [x] |
| 20 | PATCH /api/tickets/{ticketCode}/use | E2ETKT06 | [x] |
| 21 | POST /api/auth/login | E2EAUTH01 | [x] |
| 22 | GET /api/auth/me | E2EAUTH02 | [x] |

Rutas cubiertas: 22 de 22.

## 8. Resumen por módulo

| Módulo | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| Conectividad | 1 | 0 | 0 | 0 |
| Venues / Artists (Flujo A) | 11 | 0 | 0 | 0 |
| Auth / usuarios / JWT (Flujo B) | 31 | 0 | 0 | 0 |
| Events (Flujo C) | 21 | 0 | 0 | 0 |
| Tickets (Flujo D) | 28 | 0 | 0 | 0 |
| Reglas de negocio (409) | 7 | 0 | 0 | 0 |
| Errores HTTP | 4 | 0 | 0 | 0 |
| **Total** | **103** | **0** | **0** | **0** |

## 9. Defectos y bloqueos

Ninguno: no hay casos en FAIL ni BLOCKED.

| # | Caso | Precondición | Pasos | HTTP esperado vs. observado | Cómo reproducir |
|---|---|---|---|---|---|

### Hallazgos de diseño (no son FAIL)

Ningún caso de la bitácora los da por incorrectos.

| # | Hallazgo | Dónde | Efecto | Decisión pendiente |
|---|---|---|---|---|
| H1 | Un evento SOLD_OUT deja de ser público | `EventServiceImpl`: `PUBLIC_STATUSES` solo contiene PUBLISHED | Tras "Aforo: A compra CAP1", EVT-E2E-CAP1 desaparece de `/api/events/published` y su detalle da 404 a anónimos y USER. ADMIN y STAFF lo ven con 200 | Definir si SOLD_OUT debe verse en el catálogo (el código ya prevé ampliar la lista) |
| H2 | Cancelar un ticket no reabre un evento SOLD_OUT | `TicketServiceImpl.cancel` no revisa el estado del evento | Si se cancela el ticket de EVT-E2E-CAP1, el cupo queda libre pero el evento sigue SOLD_OUT y nadie puede comprar. No lo ejercita ningún caso | Definir si hay que volver a PUBLISHED |

## 10. Confirmaciones pendientes

Todas resueltas (detalle en la columna Observación de cada caso):

1. Registros de A y B: 201 y sin password ni hash. Resuelta.
2. `/me` de STAFF: `roles` = `["STAFF"]`. Resuelta.
3. TC-AUTH009: el token original sin alterar da 200. Resuelta.
4. TC-AUTH012: con `userAToken` vigente sigue en 401. Resuelta.
5. TC-AUTH014 integridad: con `adminToken` vigente sigue en 404. Resuelta.
6. Registro con campo `firstName` ausente: 400. Resuelta.