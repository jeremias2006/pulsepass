/**
 * Llamadas al backend para los artistas. Las tres son públicas (permitAll).
 *
 * <p>La búsqueda por nombre artístico existe porque el backend la resuelve con una consulta JPQL
 * sin distinguir mayúsculas; se usa para enlazar un artista con los eventos en los que participa.</p>
 */
import { z } from 'zod'
import { http } from '../../lib/http'
import { ArtistaSchema } from './tipos'

export const listarArtistasActivos = () => http('/artists/active', z.array(ArtistaSchema))
export const obtenerArtista = (id: number) => http(`/artists/${id}`, ArtistaSchema)
export const obtenerArtistaPorNombre = (stageName: string) =>
  http(`/artists/by-stage-name?stageName=${encodeURIComponent(stageName)}`, ArtistaSchema)
