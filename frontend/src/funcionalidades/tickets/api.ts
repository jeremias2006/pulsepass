import { z } from 'zod'
import { http } from '../../lib/http'
import { TicketSchema, type CompraSolicitud } from './tipos'

const c = encodeURIComponent

export const comprarTicket = (datos: CompraSolicitud) =>
  http('/tickets', TicketSchema, { method: 'POST', body: datos })
export const obtenerTicket = (ticketCode: string) => http(`/tickets/${c(ticketCode)}`, TicketSchema)
export const listarTicketsDeUsuario = (email: string) =>
  http(`/tickets/by-user?email=${c(email)}`, z.array(TicketSchema))
export const listarTicketsPagadosPorEvento = (eventCode: string) =>
  http(`/events/${c(eventCode)}/tickets/paid`, z.array(TicketSchema))
export const cancelarTicket = (ticketCode: string) =>
  http(`/tickets/${c(ticketCode)}/cancel`, TicketSchema, { method: 'PATCH' })
export const usarTicket = (ticketCode: string) =>
  http(`/tickets/${c(ticketCode)}/use`, TicketSchema, { method: 'PATCH' })
