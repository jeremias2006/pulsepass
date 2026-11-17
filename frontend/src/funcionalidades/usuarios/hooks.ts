import { useMutation, useQuery } from '@tanstack/react-query'
import { obtenerUsuarioPorEmail, obtenerUsuarioPorUsername, registrarUsuario } from './api'

export const useRegistrarUsuario = () => useMutation({ mutationFn: registrarUsuario })

export const useUsuarioPorEmail = (email: string | undefined) =>
  useQuery({
    queryKey: ['usuario', email],
    queryFn: () => obtenerUsuarioPorEmail(email ?? ''),
    enabled: Boolean(email),
    staleTime: 60_000,
  })

export const useUsuarioPorUsername = (username: string | undefined) =>
  useQuery({
    queryKey: ['usuario', 'username', username],
    queryFn: () => obtenerUsuarioPorUsername(username ?? ''),
    enabled: Boolean(username),
    staleTime: 60_000,
  })
