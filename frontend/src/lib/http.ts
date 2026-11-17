import { z } from 'zod'

/** Contrato de error del backend (dto/response/ErrorResponse). `details` puede venir vacío. */
export const ErrorResponseSchema = z.object({
  timestamp: z.string(),
  status: z.number(),
  error: z.string(),
  message: z.string(),
  details: z.record(z.string(), z.string()).nullish(),
})

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

const BASE_URL: string = import.meta.env.VITE_API_BASE_URL ?? '/api'

interface ConfigHttp {
  obtenerToken: () => string | null
  alNoAutorizado: () => void
}

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

export async function http<T>(
  ruta: string,
  schema: z.ZodType<T>,
  { method = 'GET', body, signal, ignorar401 = false }: OpcionesHttp = {},
): Promise<T> {
  const token = config.obtenerToken()
  const headers: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) headers['Content-Type'] = 'application/json'
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
    if (e instanceof DOMException && e.name === 'AbortError') throw e
    throw new ApiError(0, 'No pudimos conectar con el servidor.')
  }

  if (!res.ok) {
    const parsed = ErrorResponseSchema.safeParse(await res.json().catch(() => null))
    // Solo si ESTA petición llevaba token: un 401 sin token (anónimo) no es "sesión expirada".
    if (res.status === 401 && token && !ignorar401) config.alNoAutorizado()
    if (parsed.success) {
      throw new ApiError(res.status, parsed.data.message, parsed.data.details ?? {})
    }
    throw new ApiError(res.status, 'Ocurrió un error inesperado. Inténtalo más tarde.')
  }

  return schema.parse(await res.json())
}
