# PulsePass — Pruebas E2E manuales con Postman (Fase 5)

Guía para ejecutar, **a mano y desde la interfaz de Postman**, las pruebas HTTP de caja negra de PulsePass contra la aplicación real conectada a PostgreSQL. No hay scripts, Collection Runner, Newman ni cURL: cada solicitud se envía con **Send**, se inspecciona la respuesta y el resultado se anota en la bitácora.

Alcance: 22 rutas (20 de negocio + `POST /api/auth/login` + `GET /api/auth/me`), tres roles (USER, STAFF, ADMIN), visitante anónimo y un USER ajeno.

## 1. Contenido de esta carpeta

| Archivo | Para qué sirve |
|---|---|
| `postman/PulsePass-Manual-E2E.postman_collection.json` | Colección Postman v2.1, 98 solicitudes organizadas por carpetas, sin scripts. |
| `postman/PulsePass-local.postman_environment.json` | Plantilla de variables. Los secretos van vacíos y solo se llenan dentro de Postman. |
| `fixtures/seed-e2e.sql` | Venues y eventos de prueba (solo para la base E2E). |
| `fixtures/README.md` | Qué datos crea cada fixture y cómo preparar los usuarios. |
| `checklist-e2e-manual.md` | Orden recomendado de ejecución. |
| `registro-ejecucion-manual.md` | Bitácora: esperado vs. observado, PASS / FAIL / BLOCKED / NOT RUN. |

## 2. Preparar el ambiente

Usa una base **dedicada** (`pulsepass_e2e`). Nunca apuntes estas pruebas a una base de desarrollo, QA o producción.

### 2.1 Base de datos

Ejemplo con Docker (ajusta nombre y credenciales a las tuyas):

```
docker run --name pulsepass-e2e-db -e POSTGRES_PASSWORD=postgres -e POSTGRES_DB=pulsepass_e2e -p 5432:5432 -d postgres:16
```

### 2.2 Variables de entorno de la aplicación

La aplicación lee estos valores del entorno (ver `.env.example`). Defínelos en la configuración de ejecución de IntelliJ o en la terminal; **no los guardes en el repositorio**.

| Variable | Valor |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `local` (activa `LocalAccountsSeeder`, que crea `admin` y `laura`). |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/pulsepass_e2e` |
| `SPRING_DATASOURCE_USERNAME` / `SPRING_DATASOURCE_PASSWORD` | Los de tu PostgreSQL de pruebas. |
| `PULSEPASS_JWT_SECRET_BASE64` | Clave aleatoria exclusiva de E2E, de al menos 32 bytes. Por ejemplo: `openssl rand -base64 32`. |
| `PULSEPASS_JWT_ISSUER` | `pulsepass-api` |
| `PULSEPASS_BOOTSTRAP_ADMIN_PASSWORD` | Frase de 12 a 72 caracteres para el usuario `admin`. |
| `PULSEPASS_BOOTSTRAP_STAFF_PASSWORD` | Frase de 12 a 72 caracteres para el usuario `laura` (STAFF). |

### 2.3 Arranque y datos

1. Arranca la aplicación (`PulsepassApplication` o `./mvnw spring-boot:run`). Flyway aplica las migraciones V1–V4 y el seeder crea `admin` y `laura` si las contraseñas son válidas.
2. Con la app ya levantada (las tablas existen), carga los fixtures. Ver `fixtures/README.md`.
3. Comprueba la conectividad: `GET {{baseUrl}}/api/venues/active` debe devolver 200 con `VEN-E2E-01` y `VEN-E2E-CAP1`.

## 3. Importar y configurar Postman

1. Importa los dos archivos de `postman/` (colección y ambiente) y **selecciona el ambiente `PulsePass-local`** arriba a la derecha.
2. Completa solo en Postman los valores de las variables tipo secreto: `userAPassword`, `userBPassword`, `userCPassword`, `adminPassword`, `staffPassword`. Usa contraseñas de 12 a 72 caracteres.
3. Si repites la prueba desde cero, cambia `runId` y actualiza los datos derivados (correos, usernames y `newEventCode`) para evitar colisiones.

**Antes de compartir o exportar nada, vacía las variables secretas y de token.**

## 4. Cómo se ejecuta cada solicitud

1. Abre la solicitud. Su descripción indica ID, actor, precondición, HTTP esperado y qué revisar.
2. Comprueba qué token usa en *Authorization* (por ejemplo `{{userAToken}}`) y que siga vigente.
3. Pulsa **Send**.
4. Inspecciona **Status**, **Headers** (`Content-Type: application/json`) y **Body**.
5. En operaciones que cambian estado o en denegaciones, haz un `GET` posterior con un actor autorizado y comprueba que el estado es el esperado (o que **no** cambió).
6. Anota el resultado en `registro-ejecucion-manual.md`.

### Copia manual de valores

| Qué copiar | De | A la variable |
|---|---|---|
| `accessToken` | Respuesta de `POST /api/auth/login` | `userAToken`, `userBToken`, `userCToken`, `staffToken` o `adminToken`, según el actor |
| `id` y `stageName` de un artista | `GET /api/artists/active` | `artistId`, `artistStageName` |
| `ticketCode` del primer ticket (se cancelará) | Respuesta de `POST /api/tickets` | `ticketCancelableCode` |
| `ticketCode` del segundo ticket (se marcará como usado) | Respuesta del segundo `POST /api/tickets` | `ticketUsableCode` |

Los tokens duran 15 minutos (`expiresIn` 900). Si una solicitud protegida da 401 inesperado, vuelve a hacer login y actualiza la variable antes de dar un FAIL.

## 5. Criterios de resultado

| Resultado | Cuándo |
|---|---|
| **PASS** | HTTP, JSON y condición de negocio/seguridad coinciden con lo esperado. |
| **FAIL** | Algo no coincide, o hay un 500 no esperado. |
| **BLOCKED** | No se puede ejecutar por falta de fixture, actor, token o dato previo. |
| **NOT RUN** | Aún no se ha ejecutado. |

Un 2xx por sí solo no basta: revisa el contenido y el efecto de negocio. Todo error debe tener `ErrorResponse` con `timestamp`, `status`, `error`, `message` y `details`, con `status` igual al HTTP y sin stack traces, hashes, tokens ni secretos.

## 6. Políticas de acceso que el código implementa

Estas son las políticas contra las que se comparan los casos de propiedad (variable `expectedForeignResourceStatus` = `403`):

| Situación | Resultado esperado |
|---|---|
| Recurso ajeno de usuario o ticket (USER_B sobre datos de USER_A) | **403**. Las reglas `@PreAuthorize` se evalúan antes que la consulta. |
| Borrador de evento para anónimo o USER | **404**, igual que si no existiera. |
| Borrador de evento para STAFF o ADMIN | 200. |
| Crear, publicar o asociar artista sin ser ADMIN | 401 sin token, 403 con USER o STAFF. |
| Comprar ticket | Solo rol USER y solo con su propio `userEmail`. ADMIN y STAFF reciben 403. |
| Cancelar ticket | ADMIN, o USER dueño del ticket. STAFF recibe 403. |
| Marcar ticket como usado / listar pagados | STAFF o ADMIN. |
| Regla de negocio violada (evento no publicado, aforo agotado, edad insuficiente, transición inválida, asociación repetida) | **409**. |

## 7. Orden de ejecución

Sigue `checklist-e2e-manual.md` (equivale a los Flujos A–E del PRD). Reglas que evitan resultados ambiguos:

- Las **denegaciones** (403) sobre un recurso se ejecutan **antes** de cualquier transición de estado de ese recurso.
- `PATCH .../publish` sobre `eventToPublishCode` (solicitud `E2EEVT04-fix`) se envía **al final** del Flujo C, después de las negativas `TC-SEC002a/b`, `TC-AUTH014` y de comprobar que sigue en `DRAFT`.
- Usa un ticket distinto para cada operación irreversible: T1 para cancelar, T2 para usar. No reutilices T1 para `use` salvo en el caso de "usar ticket cancelado" (409).
- Si falla un prerrequisito, marca los casos dependientes como BLOCKED, no como PASS.

## 8. Repetir las pruebas

Restaura la base `pulsepass_e2e` (borrar y recrear), vuelve a arrancar la app para que Flyway y el seeder corran, recarga `seed-e2e.sql` y cambia `runId`. **Verifica el nombre de la base antes de borrar nada.**

## 9. Confidencialidad

- No pegues JWT, contraseñas ni datos personales reales en la bitácora ni en capturas.
- No versiones `.env`, ambientes exportados con valores ni evidencias con credenciales (ver `.gitignore` de esta carpeta).
- Para los casos de JWT con clave ajena o token vencido usa solo claves inventadas de prueba; nunca el secreto real de la app.

## 10. Limitación conocida del MVP

El rol STAFF es global: puede marcar como usado cualquier ticket de cualquier evento y listar los pagados de cualquier evento. Es una limitación heredada de la Fase 4, no un diseño recomendado para producción.
