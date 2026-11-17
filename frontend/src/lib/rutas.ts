export const RUTAS = {
  inicio: '/',
  eventos: '/eventos',
  evento: (eventCode: string) => `/eventos/${encodeURIComponent(eventCode)}`,
  artistas: '/artistas',
  artista: (id: number | string) => `/artistas/${id}`,
  sedes: '/sedes',
  sede: (code: string) => `/sedes/${encodeURIComponent(code)}`,
  login: '/login',
  registro: '/registro',
  cuenta: '/cuenta',
  misTickets: '/mis-tickets',
  ticket: (ticketCode: string) => `/mis-tickets/${encodeURIComponent(ticketCode)}`,
  confirmacionCompra: (ticketCode: string) =>
    `/compra/confirmacion/${encodeURIComponent(ticketCode)}`,
  staffValidacion: '/staff/validacion',
  staffTicketsPagados: (eventCode: string) =>
    `/staff/eventos/${encodeURIComponent(eventCode)}/tickets`,
  adminEventos: '/admin/eventos',
  adminNuevoEvento: '/admin/eventos/nuevo',
  adminEvento: (eventCode: string) => `/admin/eventos/${encodeURIComponent(eventCode)}`,
  sinPermisos: '/403',
} as const

/**
 * Lee `state.desde` (la ruta a la que volver tras iniciar sesión). Todo dato externo se valida:
 * solo se acepta una ruta interna ("/algo"), nunca "//host" ni una URL absoluta.
 */
export function desdeSeguro(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'desde' in state) {
    const d = (state as { desde: unknown }).desde
    if (typeof d === 'string' && d.startsWith('/') && !d.startsWith('//')) return d
  }
  return RUTAS.inicio
}
