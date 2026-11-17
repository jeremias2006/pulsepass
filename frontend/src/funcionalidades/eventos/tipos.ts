import { z } from 'zod'
import { ArtistaSchema } from '../artistas/tipos'

export const EventCategorySchema = z.enum([
  'MUSIC',
  'SPORTS',
  'TECHNOLOGY',
  'EDUCATION',
  'CULTURE',
  'ENTERTAINMENT',
])
export const EventStatusSchema = z.enum(['DRAFT', 'PUBLISHED', 'SOLD_OUT', 'CANCELLED', 'FINISHED'])
export type EventCategory = z.infer<typeof EventCategorySchema>
export type EventStatus = z.infer<typeof EventStatusSchema>

/** dto/response/EventSummaryResponse (lista). Solo trae el nombre de la sede. */
export const EventoResumenSchema = z.object({
  id: z.number(),
  eventCode: z.string(),
  name: z.string(),
  category: EventCategorySchema,
  status: EventStatusSchema,
  eventDate: z.string(), // LocalDate "YYYY-MM-DD" (sin hora)
  venueName: z.string(),
})
export type EventoResumen = z.infer<typeof EventoResumenSchema>

/** dto/response/EventResponse (detalle). */
export const EventoSchema = EventoResumenSchema.extend({
  description: z.string().nullish(),
  minimumAge: z.number().nullish(),
  venueCode: z.string(),
  artists: z.array(ArtistaSchema),
})
export type Evento = z.infer<typeof EventoSchema>

/** Formulario "Crear evento": límites = los de CreateEventRequest del backend. */
export const CrearEventoSchema = z.object({
  eventCode: z.string().trim().min(1, 'Ingresa el código').max(30, 'Máximo 30 caracteres'),
  name: z.string().trim().min(1, 'Ingresa el nombre').max(150, 'Máximo 150 caracteres'),
  description: z.string().max(1000, 'Máximo 1000 caracteres'),
  category: EventCategorySchema,
  eventDate: z.string().min(1, 'Selecciona la fecha'),
  minimumAge: z
    .number({ error: 'Ingresa un número' })
    .int('Debe ser un entero')
    .min(0, 'Debe ser 0 o mayor'),
  venueCode: z.string().min(1, 'Selecciona una sede'),
})
export type CrearEventoForm = z.infer<typeof CrearEventoSchema>

/** Lo que se envía a POST /api/events: una descripción vacía se omite. */
export type CrearEventoSolicitud = Omit<CrearEventoForm, 'description'> & { description?: string }
