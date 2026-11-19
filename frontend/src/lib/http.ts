/**
 * Única puerta de salida hacia el backend: todo el frontend llama a la API a través de `http`.
 *
 * <p>Centralizar aquí el `fetch` permite resolver en un solo sitio lo que de otro modo habría que
 * repetir en cada llamada: pegar el token JWT, serializar y deserializar JSON, traducir los
 * errores al tipo {@link ApiError}, validar la respuesta con Zod y disparar el cierre de sesión
 * ante un 401. Ningún otro archivo del proyecto debe usar `fetch` directamente.</p>
 *
 * <p>Equivalencia con el backend: este archivo cumple el papel del driver JDBC — es el transporte
 * de bajo nivel, y las capas de arriba (los `api.ts` de cada funcionalidad) solo declaran la ruta
 * y el molde de datos esperado.</p>
 */
import { z } from 'zod'

/** Contrato de error del backend (dto/response/ErrorResponse). `details` puede venir vacío. */
export const ErrorResponseSchema = z.object({
  timestamp: z.string(),
  status: z.number(),
  error: z.string(),
  message: z.string(),
  details: z.record(z.string(), z.string()).nullish(),
})

/**
 * Error de la API con el código HTTP a la vista, para que quien llame pueda decidir según él
 * (ver `mensajeDeError` y `aplicarErroresServidor` en lib/errores.ts).
 *
 * `status = 0` es la convención del proyecto para "no hubo respuesta": el servidor está caído
 * o falló la red, así que no existe ningún código HTTP que reportar.
 */
// Campos explícitos (no `constructor(public status…)`): la plantilla activa erasableSyntaxOnly.
export class ApiError extends Error {
  readonly status: number
  readonly details: Record<string, string>

  constructor(status: number, message: string, details: Record<string, string> = {}) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
  }
}

// Prefijo de todas las rutas. Viene de la variable de entorno de Vite, así que el mismo código
// sirve en desarrollo (http://localhost:8080/api) y en producción. Por defecto "/api", que
// funciona cuando el front se sirve detrás del mismo dominio que la API.
// Por eso los `api.ts` escriben rutas cortas ("/events/published") y no la URL completa.
const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api'

/**
 * Inyección de dependencias a mano: este módulo necesita el token y necesita avisar cuando la
 * sesión caduca, pero NO puede importar AuthProvider (sería una dependencia circular, porque
 * AuthProvider importa `http` para llamar a /auth/login y /auth/me).
 *
 * La solución es que AuthProvider le "enchufe" las dos funciones al arrancar, con
 * `configurarHttp`. Es el mismo principio que la inyección por constructor del backend: este
 * archivo declara qué necesita y otro se lo entrega.
 */
interface ConfigHttp {
  /** Devuelve el token vigente, o null si no hay sesión. */
  obtenerToken: () => string | null
  /** Se invoca cuando el backend rechaza un token: cierra la sesión en el frontend. */
  alNoAutorizado: () => void
}

// Valores inertes hasta que AuthProvider llame a configurarHttp: sin sesión no hay token
// y no hay nada que cerrar. Así las peticiones públicas funcionan igual.
let config: ConfigHttp = { obtenerToken: () => null, alNoAutorizado: () => {} }

/** Lo llama AuthProvider para enchufar el token actual y el cierre de sesión ante un 401. */
export function configurarHttp(nueva: ConfigHttp): void {
  config = nueva
}

export interface OpcionesHttp {
  method?: 'GET' | 'POST' | 'PATCH'
  body?: unknown
  signal?: AbortSignal
  /** true en el login: allí un 401 significa "credenciales inválidas", no "sesión expirada". */
  ignorar401?: boolean
}

/**
 * Ejecuta una llamada a la API y devuelve la respuesta ya validada y tipada.
 *
 * @param ruta   ruta relativa a BASE_URL, p. ej. "/events/published"
 * @param schema esquema Zod de la respuesta esperada; también define el tipo de retorno
 * @throws ApiError ante cualquier respuesta que no sea 2xx, o si no hubo respuesta (status 0)
 */
export async function http<T>(
  ruta: string,
  schema: z.ZodType<T>,
  { method = 'GET', body, signal, ignorar401 = false }: OpcionesHttp = {},
): Promise<T> {
  const token = config.obtenerToken()
  const headers: Record<string, string> = { Accept: 'application/json' }
  // Content-Type solo cuando se envía cuerpo: un GET sin cuerpo no debe declararlo.
  if (body !== undefined) headers['Content-Type'] = 'application/json'
  // El token viaja en la cabecera Authorization con el prefijo "Bearer ", que es exactamente
  // lo que espera JwtAuthenticationFilter en el backend. Nunca en la URL: las direcciones
  // quedan registradas en historiales, logs y cabeceras Referer; las cabeceras no.
  if (token) headers.Authorization = `Bearer ${token}` // solo en la cabecera, jamás en la URL

  let res: Response
  try {
    res = await fetch(`${BASE_URL}${ruta}`, {
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal,
    })
  } catch (e: unknown) {
    // Una cancelación (el usuario cambió de pantalla antes de recibir la respuesta) no es un
    // error: se relanza tal cual para que React Query la reconozca y la ignore en silencio.
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    // Cualquier otro fallo aquí significa que la petición no llegó: red caída o backend apagado.
    throw new ApiError(0, 'No pudimos conectar con el servidor.')
  }

  // A partir de aquí hubo respuesta HTTP; `res.ok` es true para los códigos 2xx.
  if (!res.ok) {
    // El backend responde los errores con el formato único de ErrorResponse (lo arma
    // GlobalExceptionHandler, o SecurityErrorWriter cuando el fallo ocurre en los filtros).
    // Se usa safeParse y no parse: si el cuerpo no tiene ese formato (p. ej. un error del
    // servidor web), no debe reventar aquí sino degradar al mensaje genérico de más abajo.
    const parsed = ErrorResponseSchema.safeParse(await res.json().catch(() => null))
    // Cierre de sesión automático ante un 401, con dos salvedades:
    //   `token`      → un 401 en una petición anónima no es una sesión expirada, es que la
    //                  ruta exigía autenticación y nunca la hubo.
    //   !ignorar401  → en el login un 401 significa "credenciales inválidas"; cerrar sesión
    //                  ahí mostraría el aviso equivocado.
    if (res.status === 401 && token && !ignorar401) config.alNoAutorizado()
    if (parsed.success) {
      // Se conserva `details` (el mapa campo → mensaje de los 400 de Bean Validation) para
      // que los formularios puedan marcar cada campo. Ver aplicarErroresServidor.
      throw new ApiError(res.status, parsed.data.message, parsed.data.details ?? {})
    }
    throw new ApiError(res.status, 'Ocurrió un error inesperado. Inténtalo más tarde.')
  }

  // Validación de la respuesta: si el backend cambiara un campo o un tipo, el fallo salta aquí,
  // en el borde de la aplicación, y no más adelante como un `undefined` dentro de un componente.
  // Además `schema.parse` devuelve el valor ya tipado, que es lo que React Query guarda en la
  // caché y las pantallas leen como `consulta.data`.
  return schema.parse(await res.json())
}
