import { z } from 'zod'

export const TicketTypeSchema = z.enum(['GENERAL', 'VIP', 'BACKSTAGE', 'STUDENT'])
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

/** dto/request/PurchaseTicketRequest. `userEmail` SIEMPRE sale de la sesión (anti-IDOR). */
export interface CompraSolicitud {
  userEmail: string
  eventCode: string
  type: TicketType
}
