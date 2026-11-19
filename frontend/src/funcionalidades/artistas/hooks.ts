/**
 * Hooks de artistas. Solo lecturas, con `staleTime` de 5 minutos por la misma razón que las
 * sedes: son datos de catálogo que casi no cambian.
 *
 * <p>`useArtistaPorNombre` existe para la pantalla de detalle del artista, que necesita el
 * nombre artístico para pedir después sus eventos con `useEventosPorArtista`.</p>
 */
import { useQuery } from '@tanstack/react-query'
import { listarArtistasActivos, obtenerArtista, obtenerArtistaPorNombre } from './api'

export const useArtistasActivos = () =>
  useQuery({
    queryKey: ['artistas', 'activos'],
    queryFn: listarArtistasActivos,
    staleTime: 300_000,
  })

/** `id` ya validado como entero positivo por la página; si es null no se llama a la API. */
export const useArtista = (id: number | null) =>
  useQuery({
    queryKey: ['artista', id],
    queryFn: () => obtenerArtista(id ?? 0),
    enabled: id !== null,
    staleTime: 300_000,
  })

export const useArtistaPorNombre = (stageName: string | null) =>
  useQuery({
    queryKey: ['artista', 'nombre', stageName],
    queryFn: () => obtenerArtistaPorNombre(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: 300_000,
  })
