/**
 * Esquema Zod de los artistas, espejo de ArtistResponse.
 *
 * <p>El DTO del backend expone muy pocos campos, y de ahí vienen dos decisiones visibles en la
 * interfaz: como no hay foto, `AvatarArtista` dibuja las iniciales; y como no hay género, la
 * clasificación visual se hace por la categoría del evento, no por el artista.</p>
 */
import { z } from 'zod'

/** dto/response/ArtistResponse: solo id, stageName y active (sin género ni foto). */
export const ArtistaSchema = z.object({
  id: z.number(),
  stageName: z.string(),
  active: z.boolean(),
})
export type Artista = z.infer<typeof ArtistaSchema>
