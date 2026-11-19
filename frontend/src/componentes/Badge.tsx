/**
 * Etiquetas de estado para eventos y tickets.
 *
 * <p>Es la excepción de esta carpeta: a diferencia del resto de `componentes/`, sí conoce el
 * dominio, porque importa los enums EventStatus y TicketStatus. Está aquí, y no dentro de una
 * funcionalidad, porque lo usan pantallas de las dos áreas y porque las dos variantes deben
 * compartir exactamente la misma forma.</p>
 *
 * <p>Los mapas traducen el estado del backend a español y le asignan un color con significado:
 * verde para lo correcto (publicado, pagado), ámbar para la advertencia (agotado), rojo para lo
 * cancelado, gris para lo inactivo. El color nunca va solo: siempre hay texto, para quien no
 * distingue colores.</p>
 */
import clsx from 'clsx'
import type { EventStatus } from '../funcionalidades/eventos/tipos'
import type { TicketStatus } from '../funcionalidades/tickets/tipos'

const BASE = 'inline-block rounded-full px-2.5 py-0.5 text-xs font-semibold'

// Colores de PRD §9.1 (mapas completos, sin clases dinámicas).
// "Mapa completo" = Record<EventStatus, …>: si el backend añadiera un estado, TypeScript
// señalaría este objeto como incompleto. "Sin clases dinámicas" = las clases están escritas
// enteras, porque Tailwind solo genera el CSS de las que encuentra literalmente en el código.
const ESTADO_EVENTO: Record<EventStatus, { etiqueta: string; clases: string }> = {
  DRAFT: {
    etiqueta: 'Borrador',
    clases: 'bg-slate-200 text-slate-700 dark:bg-slate-700 dark:text-slate-200',
  },
  PUBLISHED: {
    etiqueta: 'Publicado',
    clases: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200',
  },
  SOLD_OUT: {
    etiqueta: 'Agotado',
    clases: 'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-200',
  },
  CANCELLED: {
    etiqueta: 'Cancelado',
    clases: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  },
  FINISHED: {
    etiqueta: 'Finalizado',
    clases: 'bg-slate-300 text-slate-800 dark:bg-slate-600 dark:text-slate-100',
  },
}

const ESTADO_TICKET: Record<TicketStatus, { etiqueta: string; clases: string }> = {
  RESERVED: {
    etiqueta: 'Reservado',
    clases: 'bg-sky-100 text-sky-800 dark:bg-sky-900 dark:text-sky-200',
  },
  PAID: {
    etiqueta: 'Pagado',
    clases: 'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-200',
  },
  USED: {
    etiqueta: 'Usado',
    clases: 'bg-slate-300 text-slate-800 dark:bg-slate-600 dark:text-slate-100',
  },
  CANCELLED: {
    etiqueta: 'Cancelado',
    clases: 'bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-200',
  },
}

export function BadgeEstadoEvento({ estado }: { estado: EventStatus }) {
  const e = ESTADO_EVENTO[estado]
  return <span className={clsx(BASE, e.clases)}>{e.etiqueta}</span>
}

export function BadgeEstadoTicket({ estado }: { estado: TicketStatus }) {
  const e = ESTADO_TICKET[estado]
  return <span className={clsx(BASE, e.clases)}>{e.etiqueta}</span>
}

export function BadgeNeutro({ children }: { children: string }) {
  return (
    <span
      className={clsx(BASE, 'bg-marca-50 text-marca-700 dark:bg-marca-700/40 dark:text-marca-50')}
    >
      {children}
    </span>
  )
}
