import { z } from 'zod'

/** dto/response/VenueResponse (sin dirección). */
export const SedeSchema = z.object({
  id: z.number(),
  code: z.string(),
  name: z.string(),
  city: z.string(),
  capacity: z.number(),
  active: z.boolean(),
})
export type Sede = z.infer<typeof SedeSchema>
