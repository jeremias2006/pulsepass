import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  asociarArtista,
  crearEvento,
  listarEventosPorArtista,
  listarEventosPublicados,
  obtenerEvento,
  publicarEvento,
} from './api'

export const useEventosPublicados = () =>
  useQuery({
    queryKey: ['eventos', 'publicados'],
    queryFn: listarEventosPublicados,
    staleTime: 60_000,
  })

export const useEventosPorArtista = (stageName: string | null) =>
  useQuery({
    queryKey: ['eventos', 'artista', stageName],
    queryFn: () => listarEventosPorArtista(stageName ?? ''),
    enabled: Boolean(stageName),
    staleTime: 60_000,
  })

export const useEvento = (eventCode: string | undefined) =>
  useQuery({
    queryKey: ['evento', eventCode],
    queryFn: () => obtenerEvento(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 30_000,
  })

export function useCrearEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: crearEvento,
    onSuccess: (evento) => qc.setQueryData(['evento', evento.eventCode], evento), // siembra la caché
  })
}

export function usePublicarEvento() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: publicarEvento,
    onSuccess: async (evento) => {
      qc.setQueryData(['evento', evento.eventCode], evento)
      await qc.invalidateQueries({ queryKey: ['eventos', 'publicados'] })
    },
  })
}

export function useAsociarArtista(eventCode: string) {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: (artistId: number) => asociarArtista(eventCode, artistId),
    onSuccess: async (evento) => {
      qc.setQueryData(['evento', eventCode], evento)
      await qc.invalidateQueries({ queryKey: ['eventos', 'artista'] })
    },
  })
}
