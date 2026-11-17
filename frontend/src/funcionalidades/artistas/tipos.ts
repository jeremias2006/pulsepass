import { z } from 'zod'

/** dto/response/ArtistResponse: solo id, stageName y active (sin género ni foto). */
export const ArtistaSchema = z.object({
  id: z.number(),
  stageName: z.string(),
  active: z.boolean(),
})
export type Artista = z.infer<typeof ArtistaSchema>
