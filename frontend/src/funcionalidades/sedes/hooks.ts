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
