import { z } from 'zod'
import { http } from '../../lib/http'
import { ArtistaSchema } from './tipos'

export const listarArtistasActivos = () => http('/artists/active', z.array(ArtistaSchema))
export const obtenerArtista = (id: number) => http(`/artists/${id}`, ArtistaSchema)
export const obtenerArtistaPorNombre = (stageName: string) =>
  http(`/artists/by-stage-name?stageName=${encodeURIComponent(stageName)}`, ArtistaSchema)
