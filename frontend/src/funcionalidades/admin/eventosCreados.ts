/**
 * Apaño del frontend para una carencia conocida del backend (GAP-01).
 *
 * <p>No existe ningún endpoint que liste todos los eventos: solo `GET /events/published`. Por
 * tanto un ADMIN no tiene forma de encontrar los eventos en DRAFT que acaba de crear, y sin
 * conocer su código no puede volver a abrirlos para asociar artistas o publicarlos.</p>
 *
 * <p>Como solución provisional se recuerdan en sessionStorage, de modo que la pantalla
 * /admin/eventos pueda listarlos. Tiene dos límites evidentes y asumidos: solo funciona en esa
 * pestaña y solo con los eventos creados desde ella.</p>
 *
 * <p>La solución de fondo es del backend: añadir `GET /api/events` restringido a STAFF y ADMIN.
 * Con ese endpoint, este archivo desaparecería.</p>
 */
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

/** Añade un evento al principio de la lista, sin duplicarlo si ya estaba (el filtro lo quita antes). */
export function recordarEventoCreado(nuevo: EventoCreado): void {
  const resto = leerEventosCreados().filter((e) => e.eventCode !== nuevo.eventCode)
  sessionStorage.setItem(CLAVE_EVENTOS_CREADOS, JSON.stringify([nuevo, ...resto]))
}
