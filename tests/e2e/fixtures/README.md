# Fixtures E2E

Datos de prueba **exclusivos** de la base `pulsepass_e2e`. No cargar nunca en desarrollo, QA ni producción.

## Orden de preparación

1. Arrancar la aplicación una vez para que Flyway cree las tablas (migraciones V1–V4) y `LocalAccountsSeeder` cree `admin` y `laura` (perfil `local`).
2. Cargar `seed-e2e.sql` (inserta venues y eventos; requiere que las tablas ya existan).

Con `psql` instalado:

```
psql -U postgres -d pulsepass_e2e -f tests/e2e/fixtures/seed-e2e.sql
```

Con Docker (PowerShell):

```
Get-Content tests/e2e/fixtures/seed-e2e.sql | docker exec -i pulsepass-e2e-db psql -U postgres -d pulsepass_e2e
```

3. Verificar: `GET {{baseUrl}}/api/venues/active` debe listar `VEN-E2E-01` y `VEN-E2E-CAP1`.

El script no es idempotente: ejecutarlo dos veces falla por códigos únicos duplicados. Para repetirlo, restaurar antes la base.

## Qué crea `seed-e2e.sql`

| Tipo | Código | Estado | Detalle | Variable de Postman |
|---|---|---|---|---|
| Venue | `VEN-E2E-01` | activo | Capacidad 500 | `venueActiveCode` |
| Venue | `VEN-E2E-CAP1` | activo | Capacidad 1, para probar aforo agotado | — |
| Evento | `EVT-E2E-DRAFT` | DRAFT | Borrador, invisible para el público | `draftEventCode` |
| Evento | `EVT-E2E-PUB` | PUBLISHED | 2027-06-20, sin edad mínima | `publishedEventCode` |
| Evento | `EVT-E2E-TOPUB` | DRAFT | Para `PATCH .../publish` | `eventToPublishCode` |
| Evento | `EVT-E2E-ADULT` | PUBLISHED | Edad mínima 18 | `adultEventCode` |
| Evento | `EVT-E2E-CAP1` | PUBLISHED | En venue de capacidad 1 | `cap1EventCode` |

Las fechas de los eventos son posteriores a la fecha de ejecución; si repites la prueba mucho tiempo después, actualízalas para que sigan siendo futuras.

## Artistas

Vienen de la migración `V2__insert_initial_artists.sql`. Obtén uno con `GET /api/artists/active` y copia su `id` y `stageName` a `artistId` y `artistStageName`.

## Cuentas

| Actor | Origen | Cómo se obtiene |
|---|---|---|
| ADMIN (`admin`) | `LocalAccountsSeeder` | Contraseña de `PULSEPASS_BOOTSTRAP_ADMIN_PASSWORD`. Su rol no puede asignarse por HTTP. |
| STAFF (`laura`) | `LocalAccountsSeeder` | Contraseña de `PULSEPASS_BOOTSTRAP_STAFF_PASSWORD`. |
| USER_A, USER_B | Registro público | Solicitudes `E2EUSR01-A` y `E2EUSR01-B` (`POST /api/users`). |
| USER_C (menor de edad) | Registro público | Solicitud `TC-BIZ014-age-reg`, nacido en 2015, para el caso de edad insuficiente. |
| Usuario inactivo | Registro público + SQL | Ver abajo. |

El registro público solo crea usuarios con rol USER; un campo `role` en el cuerpo se ignora (caso `TC-SEC016`).

## Usuario inactivo (`TC-AUTH013`)

1. Haz login con un usuario ya registrado y copia su `accessToken` a `inactiveUserToken` (por ejemplo, el usuario creado en `TC-SEC016`).
2. Desactívalo en la base E2E, verificando primero que estás en `pulsepass_e2e`:

```
UPDATE users SET active = false WHERE username = 'usere_r01';
```

3. Envía `TC-AUTH013` (`GET /api/auth/me` con `inactiveUserToken`). Esperado: 401 y ningún dato del usuario.

## Artista en el borrador (`TC-SEC005`)

El seed no asocia artistas a `EVT-E2E-DRAFT`. Para comprobar que `by-artist` no expone borradores, asócialo por API con el token de ADMIN (`TC-SEC005-pre`):

```
POST {{baseUrl}}/api/events/{{draftEventCode}}/artists/{{artistId}}
```

Esperado: 200 y el evento sigue en DRAFT. Envíalo una sola vez; si lo repites responde 409. Tras restaurar la base hay que volver a enviarlo.

## Rol temporal (`TC-SEC017`)

Comprueba que un JWT emitido antes de revocar un rol pierde ese privilegio en la siguiente petición. Usa USER_C cuando ya haya terminado el caso de edad insuficiente. Confirma antes que estás conectado a `pulsepass_e2e`.

1. Asigna el rol STAFF:

```
INSERT INTO user_roles (user_id, role)
SELECT id, 'STAFF' FROM users WHERE username = 'userc_r01';
```

2. Haz login con USER_C y copia el `accessToken` a `userCToken`.
3. Envía `TC-SEC017a` (`GET /api/events/{{draftEventCode}}` con `userCToken`). Esperado: 200.
4. Revoca el rol sin volver a iniciar sesión:

```
DELETE FROM user_roles
WHERE role = 'STAFF'
  AND user_id = (SELECT id FROM users WHERE username = 'userc_r01');
```

5. Reenvía la misma petición con el mismo token (`TC-SEC017b`). Esperado: 404, porque ya solo tiene rol USER.

## Reglas

- No guardar contraseñas ni tokens reales en este directorio.
- Usar correos del dominio reservado `example.test` y un `runId` por ejecución.
- No suponer IDs autoincrementales: usar los valores que devuelve la API.