/**
 * Catálogo único de las URL internas del sitio.
 *
 * <p>Ninguna pantalla escribe una ruta a mano: todas pasan por aquí. Así un cambio de dirección
 * se hace en un solo lugar, y las rutas con parámetro son funciones que ya aplican
 * `encodeURIComponent`, de modo que un código con caracteres especiales no rompe la URL.</p>
 *
 * <p>Este objeto es el espejo en el frontend de las rutas declaradas en `router.tsx`; conviene
 * mantener los dos sincronizados.</p>
 */
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
 *
 * <p>Sin esta comprobación existiría una vulnerabilidad de redirección abierta: un enlace
 * preparado podría llevar a la víctima a otro dominio justo después de iniciar sesión, cuando
 * más confianza tiene en la página. De ahí el doble filtro: debe empezar por "/" (ruta interna)
 * pero no por "//" (que el navegador interpreta como "otro host con el mismo protocolo").</p>
 */
export function desdeSeguro(state: unknown): string {
  if (typeof state === 'object' && state !== null && 'desde' in state) {
    const d = (state as { desde: unknown }).desde
    if (typeof d === 'string' && d.startsWith('/') && !d.startsWith('//')) return d
  }
  return RUTAS.inicio
}
