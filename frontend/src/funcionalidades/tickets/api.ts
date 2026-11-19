/**
 * Llamadas al backend para los tickets: una función por endpoint, sin lógica propia.
 *
 * <p>Es la funcionalidad con más control de acceso del proyecto, y conviene recordar que NADA de
 * eso se decide aquí. El backend aplica dos capas: las reglas por URL de `SecurityConfig`
 * (comprar exige USER, validar exige STAFF o ADMIN) y las de `@PreAuthorize` en TicketService,
 * que además comprueban la propiedad del ticket con `isTicketOwner`.</p>
 */
import { z } from 'zod'
import { http } from '../../lib/http'
import { TicketSchema, type CompraSolicitud } from './tipos'

const c = encodeURIComponent

// Compra (rol USER). El cuerpo lleva el tipo de entrada, NUNCA el precio: lo calcula el servidor
// con TicketPriceCalculator. Si el precio viajara desde el cliente, cualquiera podría manipularlo.
export const comprarTicket = (datos: CompraSolicitud) =>
  http('/tickets', TicketSchema, { method: 'POST', body: datos })
// Un ticket ajeno o inexistente responde 403 en ambos casos (anti-enumeración): el frontend no
// puede ni debe distinguirlos. Ver `esNoDisponible` en lib/errores.
export const obtenerTicket = (ticketCode: string) => http(`/tickets/${c(ticketCode)}`, TicketSchema)
// El correo va como parámetro de consulta, pero el backend verifica con isSelfByEmail que sea el
// de la sesión: pedir los tickets de otra persona devuelve 403.
export const listarTicketsDeUsuario = (email: string) =>
  http(`/tickets/by-user?email=${c(email)}`, z.array(TicketSchema))
// Ruta bajo /events porque la consulta parte del evento, aunque la atiende TicketService (STAFF o ADMIN).
export const listarTicketsPagadosPorEvento = (eventCode: string) =>
  http(`/events/${c(eventCode)}/tickets/paid`, z.array(TicketSchema))
// Cancelar: solo el dueño (o un ADMIN) y solo si el ticket está PAID y el evento no pasó (BR-010/011/012).
export const cancelarTicket = (ticketCode: string) =>
  http(`/tickets/${c(ticketCode)}/cancel`, TicketSchema, { method: 'PATCH' })
// Marcar el ingreso el día del evento (STAFF o ADMIN). Al pasar a USED, el mismo ticket no
// vuelve a entrar: es la medida antifraude de la puerta.
export const usarTicket = (ticketCode: string) =>
  http(`/tickets/${c(ticketCode)}/use`, TicketSchema, { method: 'PATCH' })
