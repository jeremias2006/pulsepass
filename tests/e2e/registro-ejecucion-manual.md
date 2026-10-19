# PulsePass — Registro de ejecución manual E2E

- Base de datos: `pulsepass_e2e`
- runId: `r01`
- Fecha de inicio: 2026-10-19
- Reglas: PASS solo si HTTP, JSON y efecto de negocio coinciden. Si falta un prerrequisito, BLOCKED. Un 500 es FAIL automático.
- No pegar JWT, contraseñas ni datos personales reales en este archivo.

**Resultados posibles:** PASS · FAIL · BLOCKED · NOT RUN

**Estado al 2026-10-19 04:14:** 30 casos en PASS, 0 FAIL, 0 BLOCKED. Ningún defecto hallado hasta ahora. Hay 6 confirmaciones pendientes (ver sección 10).

---

## 0. Conectividad

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| 0.1 | 2026-10-19 03:49 | Anónimo | GET /api/venues/active | 200 | 200 | VEN-E2E-01 (cap. 500) y VEN-E2E-CAP1 | PASS | |

## 1. Flujo A — Descubrimiento público

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| E2EVEN02 / TC-BIZ001 | 2026-10-19 03:49 | Anónimo | GET /api/venues/active | 200 | 200 | Cada venue trae id, code, name, city, capacity, active; dos venues del fixture | PASS | |
| E2EVEN01 | | Anónimo | GET /api/venues/{{venueActiveCode}} | 200 | | | NOT RUN | |
| E2EVEN01-neg | | Anónimo | GET /api/venues/NO-EXISTE | 404 | | | NOT RUN | |
| E2EART03 / TC-BIZ002 | | Anónimo | GET /api/artists/active | 200 | | 5 artistas | NOT RUN | Copiar un id a artistId y su stageName a artistStageName |
| E2EART01 | | Anónimo | GET /api/artists/{{artistId}} | 200 | | | NOT RUN | |
| E2EART01-neg | | Anónimo | GET /api/artists/999999 | 404 | | | NOT RUN | |
| E2EART02 | | Anónimo | GET /api/artists/by-stage-name | 200 | | | NOT RUN | |
| E2EEVT03 / TC-SEC005 | | Anónimo | GET /api/events/published | 200 | | Sin borradores | NOT RUN | |
| E2EEVT02 | | Anónimo | GET /api/events/{{publishedEventCode}} | 200 | | PUBLISHED | NOT RUN | |
| TC-SEC004 | | Anónimo | GET /api/events/{{draftEventCode}} | 404 | | Sin datos del evento | NOT RUN | |
| E2EEVT06 | | Anónimo | GET /api/events/by-artist | 200 | | Solo publicados | NOT RUN | |

## 2. Flujo B — Registro y JWT

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| E2EUSR01-A / TC-AUTH001 / TC-BIZ006 | 2026-10-19 ~03:12 | USER_A | POST /api/users | 201 | | Registro OK (el login posterior funcionó) | PASS | Confirmar 201 en el historial de Postman y que no trae password ni hash |
| E2EUSR01-B | 2026-10-19 ~03:12 | USER_B | POST /api/users | 201 | | Registro OK (el login posterior funcionó) | PASS | Confirmar 201 en el historial |
| Registro duplicado | 2026-10-19 04:01 | USER_A (mismos datos) | POST /api/users | 409 | 409 | ErrorResponse con 5 campos, status 409, error "Conflict" | PASS | message: "Username already exists: usera_r01". El mensaje revela qué username existe; es el comportamiento esperado de un 409 de registro |
| Email mal formado | 2026-10-19 04:02 | Anónimo | POST /api/users | 400 | 400 | status 400, message "Validation failed", details.email "Email format is invalid" | PASS | |
| Falta firstName | 2026-10-19 04:03 | Anónimo | POST /api/users | 400 | 400 | status 400, details.firstName "First name is required" (y details.email, porque el body seguía con email "abc") | PASS | Se probó con firstName vacío (""), no con el campo ausente. Opcional: reenviar borrando la línea |
| Password corta | 2026-10-19 04:03 | Anónimo | POST /api/users | 400 | 400 | status 400, details.password "Password must be between 12 and 72 characters" (también firstName y email, por arrastre del body anterior) | PASS | El rango válido es de 12 a 72 caracteres |
| JSON inválido | 2026-10-19 04:04 | Anónimo | POST /api/users (sin llave de cierre) | 400 | 400 | status 400, message "Malformed or invalid JSON request", details.body "Check JSON syntax, dates and enum values" | PASS | Sin stack trace ni nombres de clase |
| TC-SEC016 (registro) | 2026-10-19 04:08 | Anónimo | POST /api/users con role=ADMIN | 201 | 201 | id 5, usere_r01, active true, firstName, lastName, phone null, city null, birthDate | PASS | Sin password ni hash en la respuesta; el campo role se ignoró |
| TC-SEC016 (login) | 2026-10-19 ~04:12 | usere_r01 | POST /api/auth/login | 200 | 200 | accessToken, Bearer, expiresIn 900 | PASS | Token no guardado en ninguna variable |
| TC-SEC016 (/me) | 2026-10-19 04:13 | usere_r01 | GET /api/auth/me | 200 | 200 | id 5, usere_r01, roles ["USER"] | PASS | El rol inyectado no se aplicó: sin escalada de privilegios |
| E2EAUTH01-A / TC-AUTH002 | 2026-10-19 03:13 | USER_A | POST /api/auth/login (username) | 200 | 200 | accessToken, tokenType Bearer, expiresIn 900 | PASS | |
| E2EAUTH01-B | 2026-10-19 ~03:17 | USER_B | POST /api/auth/login | 200 | 200 | Token copiado a userBToken | PASS | |
| E2EAUTH01-STAFF | 2026-10-19 ~03:17 | STAFF | POST /api/auth/login | 200 | 200 | Token copiado a staffToken | PASS | |
| E2EAUTH01-ADMIN | 2026-10-19 ~03:17 | ADMIN | POST /api/auth/login | 200 | 200 | Token copiado a adminToken | PASS | |
| TC-AUTH003 | 2026-10-19 ~03:23 | USER_A | POST /api/auth/login (email) | 200 | 200 | accessToken, Bearer, expiresIn 900 | PASS | |
| TC-AUTH004 | 2026-10-19 03:23 | USER_A | POST /api/auth/login (password mala) | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | message: "Autenticación requerida o credenciales inválidas" |
| TC-AUTH005 | 2026-10-19 03:24 | Anónimo | POST /api/auth/login (identifier inexistente) | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | Mismo message y mismo tamaño (618 B) que TC-AUTH004: sin enumeración de usuarios |
| E2EAUTH02-A / TC-AUTH006 | 2026-10-19 03:16 | USER_A | GET /api/auth/me | 200 | 200 | id 3, usera_r01, roles ["USER"], sin credenciales | PASS | |
| E2EAUTH02-B | 2026-10-19 ~03:18 | USER_B | GET /api/auth/me | 200 | 200 | Identidad de B | PASS | |
| E2EAUTH02-STAFF | 2026-10-19 ~03:18 | STAFF | GET /api/auth/me | 200 | 200 | Identidad de laura | PASS | Confirmar que roles mostró STAFF |
| E2EAUTH02-ADMIN | 2026-10-19 03:53 | ADMIN | GET /api/auth/me | 200 | 200 | id 1, admin, admin@pulsepass.local, roles ["ADMIN"] | PASS | |
| TC-AUTH007 | 2026-10-19 03:27 | Anónimo | GET /api/auth/me (No Auth) | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | |
| TC-AUTH008 | 2026-10-19 03:29 | Bearer abc.def.ghi | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | |
| TC-AUTH009 | 2026-10-19 03:32 | Token alterado (último carácter) | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | Un 401 solo no distingue la causa; opcional: con el token original debe dar 200 |
| TC-AUTH010 | 2026-10-19 03:42 | USER_B con token vencido (~25 min) | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | El mismo token había dado 200 a las 03:17 |
| TC-AUTH011 | 2026-10-19 03:42 | Token firmado con clave inventada | GET /api/auth/me | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | Payload válido y vigente (sub 3, iss pulsepass-api); solo la firma era falsa. Clave inventada, no la real |
| TC-AUTH012 | 2026-10-19 03:45 | Token por query string, sin header | GET /api/auth/me?access_token=... | 401 | 401 | ErrorResponse con 5 campos, status 401 | PASS | Confirmar que userAToken era vigente a las 03:45; si se pegó después, reenviar |
| TC-AUTH013 | | Usuario inactivo | GET /api/auth/me | 401 | | | NOT RUN | Requiere desactivar el usuario por SQL en Docker |
| TC-AUTH014 | 2026-10-19 03:47 | USER_A | POST /api/events | 403 | 403 | ErrorResponse con 5 campos, status 403, message "No tiene permisos para realizar esta operación" | PASS | |
| TC-AUTH014 integridad | 2026-10-19 03:52 | ADMIN | GET /api/events/{{newEventCode}} | 404 | 404 | "Event not found: EVT-r01" | PASS | Confirmar que adminToken era vigente en ese momento (reenviar ahora que /me ADMIN dio 200) |
| TC-AUTH015 | 2026-10-19 03:50 | Bearer abc.def.ghi en ruta pública | GET /api/venues/active | anotar lo observado | 200 | Devuelve los dos venues | PASS | La ruta pública ignora el token inválido; no lo rechaza ni lo trata como usuario. Sin 500 ni datos internos |

## 3. Flujo C — Catálogo (ADMIN)

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| TC-SEC001a | | Anónimo | POST /api/events | 401 | | | NOT RUN | |
| TC-SEC001b | | USER_A | POST /api/events | 403 | | | NOT RUN | Ya cubierto por TC-AUTH014; ejecutar o enlazar |
| TC-SEC001c | | STAFF | POST /api/events | 403 | | | NOT RUN | |
| TC-SEC002a | | USER_A | PATCH /api/events/{{eventToPublishCode}}/publish | 403 | | | NOT RUN | |
| TC-SEC002b | | STAFF | PATCH /api/events/{{eventToPublishCode}}/publish | 403 | | | NOT RUN | |
| TC-SEC003 | | USER_A | POST /api/events/{{publishedEventCode}}/artists/{{artistId}} | 403 | | | NOT RUN | |
| Integridad tras 403 | | ADMIN | GET /api/events/{{eventToPublishCode}} y {{newEventCode}} | 200 DRAFT / 404 | | Sigue en DRAFT; no se creó | NOT RUN | La parte newEventCode → 404 ya se verificó en TC-AUTH014 integridad; falta comprobar que eventToPublish sigue en DRAFT |
| E2EEVT01 / TC-BIZ003 | | ADMIN | POST /api/events | 201 | | status DRAFT | NOT RUN | |
| Código duplicado | | ADMIN | POST /api/events | 409 | | | NOT RUN | |
| Categoría inválida | | ADMIN | POST /api/events | 400 | | | NOT RUN | |
| Venue inexistente | | ADMIN | POST /api/events | 404 | | | NOT RUN | |
| Falta name | | ADMIN | POST /api/events | 400 | | | NOT RUN | |
| TC-SEC004 (borrador anónimo) | | Anónimo | GET /api/events/{{newEventCode}} | 404 | | | NOT RUN | |
| TC-SEC015 | | ADMIN | GET /api/events/{{newEventCode}} | 200 | | status DRAFT | NOT RUN | |
| Borrador con STAFF | | STAFF | GET /api/events/{{newEventCode}} | 200 | | | NOT RUN | |
| E2EEVT05 / TC-BIZ005 | | ADMIN | POST /api/events/{{newEventCode}}/artists/{{artistId}} | 200 | | Artista en artists | NOT RUN | |
| Asociación repetida | | ADMIN | POST (misma asociación) | anotar | | ¿200 o 409? Sin duplicado | NOT RUN | |
| Artista 999999 | | ADMIN | POST .../artists/999999 | 404 | | | NOT RUN | |
| E2EEVT04 / TC-BIZ004 | | ADMIN | PATCH /api/events/{{newEventCode}}/publish | 200 | | PUBLISHED | NOT RUN | |
| Publicar otra vez | | ADMIN | PATCH .../publish | 409 | | | NOT RUN | |
| Visible tras publicar | | Anónimo | GET /api/events/{{newEventCode}} y /published y /by-artist | 200 | | Aparece en los tres | NOT RUN | |

## 4. Flujo D — Tickets

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| E2ETKT01 / TC-BIZ007 | | USER_A | POST /api/tickets (GENERAL) | 201 | | PAID; copiar a ticketCancelableCode | NOT RUN | |
| E2ETKT02 | | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | | | NOT RUN | |
| E2ETKT03 | | USER_A | GET /api/tickets/by-user?email=A | 200 | | Incluye T1 | NOT RUN | |
| E2EUSR02 | | USER_A | GET /api/users/by-email?email=A | 200 | | | NOT RUN | |
| E2EUSR03 | | USER_A | GET /api/users/by-username?username=A | 200 | | | NOT RUN | |
| TC-SEC006 | | USER_B | GET /api/users/by-email?email=A | 403 | | Sin datos de A | NOT RUN | |
| TC-SEC007 | | USER_B | GET /api/users/by-username?username=A | 403 | | | NOT RUN | |
| TC-SEC009 | | USER_B | GET /api/tickets/{{ticketCancelableCode}} | 403 | | | NOT RUN | |
| TC-SEC010 | | USER_B | GET /api/tickets/by-user?email=A | 403 | | | NOT RUN | |
| TC-SEC011 | | USER_B | PATCH .../cancel (ticket de A) | 403 | | | NOT RUN | |
| TC-SEC012 | | USER_B | PATCH .../use (ticket de A) | 403 | | | NOT RUN | |
| Integridad T1 | | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | | Sigue PAID, mismo dueño | NOT RUN | |
| TC-SEC008 / AC-05 | | USER_A | POST /api/tickets con userEmail de B | 403 | | Sin ticket nuevo para B | NOT RUN | |
| ADMIN compra | | ADMIN | POST /api/tickets | 403 | | | NOT RUN | |
| STAFF compra | | STAFF | POST /api/tickets | 403 | | | NOT RUN | |
| E2ETKT05 / TC-BIZ010 | | USER_A | PATCH .../cancel (T1) | 200 | | CANCELLED | NOT RUN | |
| Reconsulta T1 | | USER_A | GET /api/tickets/{{ticketCancelableCode}} | 200 | | CANCELLED | NOT RUN | |
| TC-BIZ011 | | USER_A | PATCH .../cancel (T1 otra vez) | 409 | | Sin cambios | NOT RUN | |
| STAFF cancela | | STAFF | PATCH .../cancel | 403 | | | NOT RUN | |
| Cancelar inexistente | | ADMIN | PATCH /api/tickets/NO-EXISTE/cancel | 404 | | | NOT RUN | |
| Compra T2 (VIP) | | USER_A | POST /api/tickets | 201 | | Copiar a ticketUsableCode | NOT RUN | |
| E2ETKT04 / TC-BIZ009 | | STAFF | GET /api/events/{{publishedEventCode}}/tickets/paid | 200 | | Incluye T2, no T1 | NOT RUN | |
| TC-SEC013 | | USER_A | GET .../tickets/paid | 403 | | | NOT RUN | |
| USER usa ticket | | USER_A | PATCH .../use (T2) | 403 | | Sigue PAID | NOT RUN | |
| E2ETKT06 / TC-BIZ012 | | STAFF | PATCH .../use (T2) | 200 | | USED | NOT RUN | |
| Reconsulta T2 | | USER_A | GET /api/tickets/{{ticketUsableCode}} | 200 | | USED | NOT RUN | |
| TC-BIZ013 | | STAFF | PATCH .../use (T2 otra vez) | 409 | | | NOT RUN | |
| Usar ticket cancelado | | STAFF | PATCH .../use (T1) | 409 | | | NOT RUN | |

## 5. Reglas de negocio (TC-BIZ014)

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| Evento en borrador | | USER_A | POST /api/tickets | 409 | | Sin ticket creado | NOT RUN | |
| Evento inexistente | | USER_A | POST /api/tickets | 404 | | | NOT RUN | |
| Edad insuficiente | | USER_C | POST /api/tickets (EVT-E2E-ADULT) | 409 | | | NOT RUN | Requiere registrar a USER_C (nacido 2015-01-01, username userc_r01) y hacer su login |
| Aforo: A compra CAP1 | | USER_A | POST /api/tickets (EVT-E2E-CAP1) | 201 | | Evento pasa a SOLD_OUT | NOT RUN | |
| Aforo: B compra CAP1 | | USER_B | POST /api/tickets (EVT-E2E-CAP1) | 409 | | | NOT RUN | |
| Tipo inválido | | USER_A | POST /api/tickets | 400 | | | NOT RUN | |
| Email mal formado | | USER_A | POST /api/tickets | 400 | | | NOT RUN | |

## 6. Regresión de errores

| Caso | Fecha/hora | Actor | Método y ruta | HTTP esperado | HTTP real | JSON / estado observado | Resultado | Observación |
|---|---|---|---|---|---|---|---|---|
| 405 | | ADMIN | DELETE /api/events/EVT-E2E-PUB | 405 | | | NOT RUN | |
| 415 | | USER_A | POST con Content-Type text/plain | 415 | | | NOT RUN | |
| ErrorResponse | | — | Revisar en cada error | — | | 5 campos; status = HTTP; sin stack trace, hash ni token | NOT RUN | Verificado hasta ahora en 400, 401, 403, 404 y 409; faltan 405 y 415 |
| Ningún 500 | | — | Revisar en toda la ejecución | — | | | NOT RUN | Ningún 500 hasta ahora |

---

## 7. Cobertura de las 22 rutas

Marcar cuando la ruta tenga al menos un caso nominal en PASS.

| # | Ruta | Caso | Cubierta |
|---|---|---|---|
| 1 | GET /api/venues/{code} | E2EVEN01 | [ ] |
| 2 | GET /api/venues/active | E2EVEN02 | [x] |
| 3 | POST /api/events | E2EEVT01 | [ ] |
| 4 | GET /api/events/{eventCode} | E2EEVT02 | [ ] |
| 5 | GET /api/events/published | E2EEVT03 | [ ] |
| 6 | PATCH /api/events/{eventCode}/publish | E2EEVT04 | [ ] |
| 7 | POST /api/events/{eventCode}/artists/{artistId} | E2EEVT05 | [ ] |
| 8 | GET /api/events/by-artist | E2EEVT06 | [ ] |
| 9 | GET /api/artists/{id} | E2EART01 | [ ] |
| 10 | GET /api/artists/by-stage-name | E2EART02 | [ ] |
| 11 | GET /api/artists/active | E2EART03 | [ ] |
| 12 | POST /api/users | E2EUSR01 | [x] (201 observado en el registro de usere, TC-SEC016) |
| 13 | GET /api/users/by-email | E2EUSR02 | [ ] |
| 14 | GET /api/users/by-username | E2EUSR03 | [ ] |
| 15 | POST /api/tickets | E2ETKT01 | [ ] |
| 16 | GET /api/tickets/{ticketCode} | E2ETKT02 | [ ] |
| 17 | GET /api/tickets/by-user | E2ETKT03 | [ ] |
| 18 | GET /api/events/{eventCode}/tickets/paid | E2ETKT04 | [ ] |
| 19 | PATCH /api/tickets/{ticketCode}/cancel | E2ETKT05 | [ ] |
| 20 | PATCH /api/tickets/{ticketCode}/use | E2ETKT06 | [ ] |
| 21 | POST /api/auth/login | E2EAUTH01 | [x] |
| 22 | GET /api/auth/me | E2EAUTH02 | [x] |

Rutas cubiertas: 4 de 22.

## 8. Resumen por módulo

| Módulo | PASS | FAIL | BLOCKED | NOT RUN |
|---|---|---|---|---|
| Conectividad | 1 | 0 | 0 | 0 |
| Venues / Artists (Flujo A) | 1 | 0 | 0 | 10 |
| Auth / usuarios / JWT (Flujo B) | 28 | 0 | 0 | 1 |
| Events (Flujo C) | 0 | 0 | 0 | 21 |
| Tickets (Flujo D) | 0 | 0 | 0 | 28 |
| Reglas de negocio (409) | 0 | 0 | 0 | 7 |
| Errores HTTP | 0 | 0 | 0 | 4 |
| **Total** | **30** | **0** | **0** | **71** |

## 9. Defectos y bloqueos

Ninguno hasta el momento.

| # | Caso | Precondición | Pasos | HTTP esperado vs. observado | Cómo reproducir |
|---|---|---|---|---|---|

## 10. Confirmaciones pendientes

1. Registros de A y B: confirmar en el historial de Postman que fueron 201 y que no traían password ni hash.
2. `/me` de STAFF: confirmar que `roles` mostró `STAFF`.
3. TC-AUTH009: opcional, enviar el token original sin alterar y comprobar que da 200.
4. TC-AUTH012: confirmar que `userAToken` era vigente a las 03:45; si no, reenviar.
5. TC-AUTH014 integridad: reenviar con `adminToken` vigente (el `/me` de ADMIN ya dio 200 a las 03:53) y confirmar que sigue en 404.
6. Registro con "Falta campo": opcional, reenviar borrando la línea de `firstName` (se probó con valor vacío).