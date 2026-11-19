/**
 * Hooks de sedes. Solo lecturas: no hay escrituras de sedes desde el frontend.
 *
 * <p>El `staleTime` de 5 minutos (300_000 ms) es el más alto del proyecto, y es intencional: las
 * sedes cambian muy rara vez, así que no tiene sentido volver a pedirlas al navegar. Compárese
 * con los 15 segundos de los tickets.</p>
 */
import { useQuery } from '@tanstack/react-query'
import { listarSedesActivas, obtenerSede } from './api'

export const useSedesActivas = () =>
  useQuery({ queryKey: ['sedes', 'activas'], queryFn: listarSedesActivas, staleTime: 300_000 })

export const useSede = (code: string | undefined) =>
  useQuery({
    queryKey: ['sede', code],
    queryFn: () => obtenerSede(code ?? ''),
    enabled: Boolean(code),
    staleTime: 300_000,
  })
