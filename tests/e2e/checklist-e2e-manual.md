# PulsePass — Checklist de ejecución manual E2E

Orden recomendado para enviar las solicitudes de Postman. Marca cada casilla al terminar; el **estado oficial y la evidencia** viven en `registro-ejecucion-manual.md`.

## Antes de empezar

- [ ] Base `pulsepass_e2e` creada y aplicación arrancada con el perfil `local` (Flyway aplicado, `admin` y `laura` creados).
- [ ] `fixtures/seed-e2e.sql` cargado y `GET /api/venues/active` devuelve `VEN-E2E-01` y `VEN-E2E-CAP1`.
- [ ] Colección y ambiente `PulsePass-local` importados; ambiente seleccionado.
- [ ] Contraseñas de `userA`, `userB`, `userC`, `admin` y `staff` completadas **solo en Postman** (variables secretas).
- [ ] `runId` definido y datos derivados (correos, usernames, `newEventCode`) coherentes.
- [ ] Registro de USER_A, USER_B y USER_C hecho (`POST /api/users`, 201).
- [ ] Logins de A, B, C, STAFF y ADMIN hechos y tokens copiados a sus variables.

## Conectividad

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | 0.1 | Anónimo | `GET /api/venues/active` | 200 |

## Flujo A — Descubrimiento público

> Todo sin token. Copia `artistId` y `artistStageName` desde `GET /api/artists/active`.

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | E2EVEN02 / TC-BIZ001 | Anónimo | `GET /api/venues/active` | 200 |
| [ ] | E2EVEN01 | Anónimo | `GET /api/venues/{{venueActiveCode}}` | 200 |
| [ ] | E2EVEN01-neg | Anónimo | `GET /api/venues/NO-EXISTE` | 404 |
| [ ] | E2EART03 / TC-BIZ002 | Anónimo | `GET /api/artists/active` | 200 |
| [ ] | E2EART01 | Anónimo | `GET /api/artists/{{artistId}}` | 200 |
| [ ] | E2EART01-neg | Anónimo | `GET /api/artists/999999` | 404 |
| [ ] | E2EART02 | Anónimo | `GET /api/artists/by-stage-name` | 200 |
| [ ] | E2EEVT03 | Anónimo | `GET /api/events/published` | 200 |
| [ ] | E2EEVT02 | Anónimo | `GET /api/events/{{publishedEventCode}}` | 200 |
| [ ] | TC-SEC004 | Anónimo | `GET /api/events/{{draftEventCode}}` | 404 |
| [ ] | E2EEVT06 | Anónimo | `GET /api/events/by-artist` | 200 |

## Flujo B — Registro y JWT

> Inicia sesión con A, B, STAFF y ADMIN y copia cada `accessToken` a su variable. Los tokens duran 15 min: renueva si ves un 401 inesperado. `TC-AUTH013` requiere desactivar un usuario por SQL (ver `fixtures/README.md`).

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | E2EUSR01-A / TC-AUTH001 / TC-BIZ006 | USER_A | `POST /api/users` | 201 |
| [ ] | E2EUSR01-B | USER_B | `POST /api/users` | 201 |
| [ ] | Registro duplicado | USER_A (mismos datos) | `POST /api/users` | 409 |
| [ ] | Email mal formado | Anónimo | `POST /api/users` | 400 |
| [ ] | Falta firstName | Anónimo | `POST /api/users` | 400 |
| [ ] | Password corta | Anónimo | `POST /api/users` | 400 |
| [ ] | JSON inválido | Anónimo | `POST /api/users (sin llave de cierre)` | 400 |
| [ ] | TC-SEC016 (registro) | Anónimo | `POST /api/users con role=ADMIN` | 201 |
| [ ] | TC-SEC016 (login) | usere_r01 | `POST /api/auth/login` | 200 |
| [ ] | TC-SEC016 (/me) | usere_r01 | `GET /api/auth/me` | 200 |
| [ ] | E2EAUTH01-A / TC-AUTH002 | USER_A | `POST /api/auth/login (username)` | 200 |
| [ ] | E2EAUTH01-B | USER_B | `POST /api/auth/login` | 200 |
| [ ] | E2EAUTH01-STAFF | STAFF | `POST /api/auth/login` | 200 |
| [ ] | E2EAUTH01-ADMIN | ADMIN | `POST /api/auth/login` | 200 |
| [ ] | TC-AUTH003 | USER_A | `POST /api/auth/login (email)` | 200 |
| [ ] | TC-AUTH004 | USER_A | `POST /api/auth/login (password mala)` | 401 |
| [ ] | TC-AUTH005 | Anónimo | `POST /api/auth/login (identifier inexistente)` | 401 |
| [ ] | E2EAUTH02-A / TC-AUTH006 | USER_A | `GET /api/auth/me` | 200 |
| [ ] | E2EAUTH02-B | USER_B | `GET /api/auth/me` | 200 |
| [ ] | E2EAUTH02-STAFF | STAFF | `GET /api/auth/me` | 200 |
| [ ] | E2EAUTH02-ADMIN | ADMIN | `GET /api/auth/me` | 200 |
| [ ] | TC-AUTH007 | Anónimo | `GET /api/auth/me (No Auth)` | 401 |
| [ ] | TC-AUTH008 | Bearer abc.def.ghi | `GET /api/auth/me` | 401 |
| [ ] | TC-AUTH009 | Token alterado (último carácter) | `GET /api/auth/me` | 401 |
| [ ] | TC-AUTH010 | USER_B con token vencido (~25 min) | `GET /api/auth/me` | 401 |
| [ ] | TC-AUTH011 | Token firmado con clave inventada | `GET /api/auth/me` | 401 |
| [ ] | TC-AUTH012 | Token por query string, sin header | `GET /api/auth/me?access_token=...` | 401 |
| [ ] | TC-AUTH013 | Usuario inactivo | `GET /api/auth/me` | 401 |
| [ ] | TC-AUTH014 | USER_A | `POST /api/events` | 403 |
| [ ] | TC-AUTH014 integridad | ADMIN | `GET /api/events/{{newEventCode}}` | 404 |
| [ ] | TC-AUTH015 | Bearer abc.def.ghi en ruta pública | `GET /api/venues/active` | anotar lo observado |

## Flujo C — Catálogo (ADMIN)

> Ejecuta primero las **negativas** (TC-SEC001–003) y la integridad tras 403 (`eventToPublishCode` sigue en DRAFT). Después el flujo positivo con ADMIN. La solicitud `E2EEVT04-fix` (publicar `eventToPublishCode`) va al final de este flujo.

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | TC-SEC001a | Anónimo | `POST /api/events` | 401 |
| [ ] | TC-SEC001b | USER_A | `POST /api/events` | 403 |
| [ ] | TC-SEC001c | STAFF | `POST /api/events` | 403 |
| [ ] | TC-SEC002a | USER_A | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 |
| [ ] | TC-SEC002b | STAFF | `PATCH /api/events/{{eventToPublishCode}}/publish` | 403 |
| [ ] | TC-SEC003 | USER_A | `POST /api/events/{{publishedEventCode}}/artists/{{artistId}}` | 403 |
| [ ] | Integridad tras 403 | ADMIN | `GET /api/events/{{eventToPublishCode}} y {{newEventCode}}` | 200 DRAFT / 404 |
| [ ] | E2EEVT01 / TC-BIZ003 | ADMIN | `POST /api/events` | 201 |
| [ ] | TC-BIZ015-dup | ADMIN | `POST /api/events` | 409 |
| [ ] | TC-BIZ015-cat | ADMIN | `POST /api/events` | 400 |
| [ ] | TC-BIZ015-venue | ADMIN | `POST /api/events` | 404 |
| [ ] | TC-BIZ015-name | ADMIN | `POST /api/events` | 400 |
| [ ] | TC-SEC004 (borrador anónimo) | Anónimo | `GET /api/events/{{newEventCode}}` | 404 |
| [ ] | TC-SEC015 | ADMIN | `GET /api/events/{{newEventCode}}` | 200 |
| [ ] | Borrador con STAFF | STAFF | `GET /api/events/{{newEventCode}}` | 200 |
| [ ] | E2EEVT05 / TC-BIZ005 | ADMIN | `POST /api/events/{{newEventCode}}/artists/{{artistId}}` | 200 |
| [ ] | Asociación repetida | ADMIN | `POST (misma asociación)` | anotar |
| [ ] | Artista 999999 | ADMIN | `POST .../artists/999999` | 404 |
| [ ] | E2EEVT04 / TC-BIZ004 | ADMIN | `PATCH /api/events/{{newEventCode}}/publish` | 200 |
| [ ] | Publicar otra vez | ADMIN | `PATCH .../publish` | 409 |
| [ ] | Visible tras publicar | Anónimo | `GET /api/events/{{newEventCode}} y /published y /by-artist` | 200 |

## Flujo D — Tickets

> Compra T1 y T2 con USER_A. Las denegaciones de USER_B sobre T1 (SEC009–012) van **antes** de cancelar T1. T1 se cancela; T2 se marca como usado. No reutilices T1 para `use` salvo en el caso 409 de "usar ticket cancelado".

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | E2ETKT01 / TC-BIZ007 | USER_A | `POST /api/tickets (GENERAL)` | 201 |
| [ ] | E2ETKT02 / TC-BIZ008 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 |
| [ ] | E2ETKT03 / TC-BIZ008 | USER_A | `GET /api/tickets/by-user?email=A` | 200 |
| [ ] | E2EUSR02 | USER_A | `GET /api/users/by-email?email=A` | 200 |
| [ ] | E2EUSR03 | USER_A | `GET /api/users/by-username?username=A` | 200 |
| [ ] | TC-SEC006 | USER_B | `GET /api/users/by-email?email=A` | 403 |
| [ ] | TC-SEC007 | USER_B | `GET /api/users/by-username?username=A` | 403 |
| [ ] | TC-SEC009 | USER_B | `GET /api/tickets/{{ticketCancelableCode}}` | 403 |
| [ ] | TC-SEC010 | USER_B | `GET /api/tickets/by-user?email=A` | 403 |
| [ ] | TC-SEC011 | USER_B | `PATCH .../cancel (ticket de A)` | 403 |
| [ ] | TC-SEC012 | USER_B | `PATCH .../use (ticket de A)` | 403 |
| [ ] | Integridad T1 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 |
| [ ] | TC-SEC008 / AC-05 | USER_A | `POST /api/tickets con userEmail de B` | 403 |
| [ ] | ADMIN compra | ADMIN | `POST /api/tickets` | 403 |
| [ ] | STAFF compra | STAFF | `POST /api/tickets` | 403 |
| [ ] | E2ETKT05 / TC-BIZ010 | USER_A | `PATCH .../cancel (T1)` | 200 |
| [ ] | Reconsulta T1 | USER_A | `GET /api/tickets/{{ticketCancelableCode}}` | 200 |
| [ ] | TC-BIZ011 | USER_A | `PATCH .../cancel (T1 otra vez)` | 409 |
| [ ] | STAFF cancela | STAFF | `PATCH .../cancel` | 403 |
| [ ] | Cancelar inexistente | ADMIN | `PATCH /api/tickets/NO-EXISTE/cancel` | 404 |
| [ ] | Compra T2 (VIP) | USER_A | `POST /api/tickets` | 201 |
| [ ] | E2ETKT04 / TC-BIZ009 | STAFF | `GET /api/events/{{publishedEventCode}}/tickets/paid` | 200 |
| [ ] | TC-SEC013 | USER_A | `GET .../tickets/paid` | 403 |
| [ ] | TC-SEC014-pre (USER usa ticket) | USER_A | `PATCH .../use (T2)` | 403 |
| [ ] | E2ETKT06 / TC-BIZ012 / TC-SEC014 | STAFF | `PATCH .../use (T2)` | 200 |
| [ ] | Reconsulta T2 | USER_A | `GET /api/tickets/{{ticketUsableCode}}` | 200 |
| [ ] | TC-BIZ013 | STAFF | `PATCH .../use (T2 otra vez)` | 409 |
| [ ] | Usar ticket cancelado | STAFF | `PATCH .../use (T1)` | 409 |

## Reglas de negocio (TC-BIZ014)

> Requiere USER_C registrado y con token. El caso de aforo agota `EVT-E2E-CAP1`: no se puede repetir sin restaurar la base.

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | Evento en borrador | USER_A | `POST /api/tickets` | 409 |
| [ ] | Evento inexistente | USER_A | `POST /api/tickets` | 404 |
| [ ] | Edad insuficiente | USER_C | `POST /api/tickets (EVT-E2E-ADULT)` | 409 |
| [ ] | Aforo: A compra CAP1 | USER_A | `POST /api/tickets (EVT-E2E-CAP1)` | 201 |
| [ ] | Aforo: B compra CAP1 | USER_B | `POST /api/tickets (EVT-E2E-CAP1)` | 409 |
| [ ] | Tipo inválido | USER_A | `POST /api/tickets` | 400 |
| [ ] | Email mal formado | USER_A | `POST /api/tickets` | 400 |

## Regresión de errores

> Revisa `ErrorResponse` en cada error y confirma que no apareció ningún 500 durante toda la ejecución.

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | 405 | ADMIN | `DELETE /api/events/EVT-E2E-PUB` | 405 |
| [ ] | 415 | USER_A | `POST con Content-Type text/plain` | 415 |
| [ ] | ErrorResponse | — | `Revisar en cada error` | — |
| [ ] | Ningún 500 | — | `Revisar en toda la ejecución` | — |

## Casos pendientes (TC-SEC005 y TC-SEC017)

> Se envían al final. `TC-SEC005-pre` se envía una sola vez: repetirlo da 409. `TC-SEC017` necesita el cambio de rol por SQL de `fixtures/README.md` entre las dos solicitudes y usa USER_C una vez terminado el caso de edad insuficiente.

| ☐ | Caso | Actor | Solicitud | Esperado |
|---|---|---|---|---|
| [ ] | TC-SEC005-pre | ADMIN | `POST /api/events/{{draftEventCode}}/artists/{{artistId}}` | 200, sigue en DRAFT |
| [ ] | TC-SEC005a | Anónimo | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200, sin EVT-E2E-DRAFT |
| [ ] | TC-SEC005b | ADMIN | `GET /api/events/by-artist?stageName={{artistStageName}}` | 200, con EVT-E2E-DRAFT |
| [ ] | TC-SEC017a | USER_C con STAFF temporal | `GET /api/events/{{draftEventCode}}` | 200 |
| [ ] | TC-SEC017b | USER_C, mismo token, STAFF revocado | `GET /api/events/{{draftEventCode}}` | 404 |

## Cierre

- [ ] Los 22 endpoints tienen al menos un caso nominal en PASS (sección 7 del registro).
- [ ] Resumen por módulo y conteos recalculados (sección 8 del registro).
- [ ] Defectos y bloqueos registrados con precondiciones, pasos y reproducción (sección 9).
- [ ] Confirmaciones pendientes resueltas o justificadas (sección 10).
- [ ] Ningún JWT, contraseña ni dato personal real en la bitácora, capturas ni archivos exportados.
- [ ] Variables de token y secretas vaciadas antes de exportar o compartir el ambiente.
- [ ] Casos omitidos marcados NOT RUN o BLOCKED; nunca como PASS.