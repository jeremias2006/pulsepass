/**
 * Esquemas Zod de los tickets, espejo de los DTO del backend.
 *
 * <p>Los cuatro tipos de entrada determinan el precio en el servidor (base 50.000 por un
 * multiplicador: GENERAL ×1, STUDENT ×0,70, VIP ×2, BACKSTAGE ×3). El frontend solo envía el
 * tipo y muestra el precio que le devuelven.</p>
 */
import { z } from 'zod'

export const TicketTypeSchema = z.enum(['GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'])
// RESERVED existe en el enum del backend pero el flujo actual no lo usa: una compra nace PAID.
// Se declara igual para que el esquema acepte cualquier valor válido del dominio.
export const TicketStatusSchema = z.enum(['RESERVED', 'PAID', 'CANCELLED', 'USED'])
export type TicketType = z.infer<typeof TicketTypeSchema>
export type TicketStatus = z.infer<typeof TicketStatusSchema>

/** dto/response/TicketResponse. Trae el correo del titular, no su nombre. */
export const TicketSchema = z.object({
  id: z.number(),
  ticketCode: z.string(),
  type: TicketTypeSchema,
  price: z.coerce.number(), // BigDecimal: llega como número (coerce tolera un string)
  status: TicketStatusSchema,
  purchaseDate: z.string(), // LocalDateTime sin zona
  userEmail: z.string(),
  eventCode: z.string(),
  eventName: z.string(),
})
export type Ticket = z.infer<typeof TicketSchema>

/**
 * dto/request/PurchaseTicketRequest. `userEmail` SIEMPRE sale de la sesión (anti-IDOR).
 *
 * <p>IDOR (referencia insegura a objeto directo) sería dejar que el cliente escriba el correo de
 * otra persona y comprar a su nombre. Por eso el valor se toma de la sesión y nunca de un campo
 * editable; y el backend lo verifica igualmente con `isSelfByEmail`, que compara los IDS de
 * usuario, no los correos.</p>
 *
 * <p>Obsérvese que NO hay campo de precio: lo calcula el servidor.</p>
 */
export interface CompraSolicitud {
  userEmail: string
  eventCode: string
  type: TicketType
}
