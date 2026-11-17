import { z } from 'zod'

/** GAP-01: la API no lista borradores. Se recuerdan, solo en esta pestaña, los eventos creados por el ADMIN. */
export const CLAVE_EVENTOS_CREADOS = 'pulsepass.eventos-creados'

const ListaSchema = z.array(z.object({ eventCode: z.string(), name: z.string() }))
export type EventoCreado = z.infer<typeof ListaSchema>[number]

export function leerEventosCreados(): EventoCreado[] {
  try {
    const r = ListaSchema.safeParse(
      JSON.parse(sessionStorage.getItem(CLAVE_EVENTOS_CREADOS) ?? '[]'),
    )
    return r.success ? r.data : []
  } catch {
    return []
  }
}

export function recordarEventoCreado(nuevo: EventoCreado): void {
  const resto = leerEventosCreados().filter((e) => e.eventCode !== nuevo.eventCode)
  sessionStorage.setItem(CLAVE_EVENTOS_CREADOS, JSON.stringify([nuevo, ...resto]))
}
