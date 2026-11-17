import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelarTicket,
  comprarTicket,
  listarTicketsDeUsuario,
  listarTicketsPagadosPorEvento,
  obtenerTicket,
  usarTicket,
} from './api'

// Invalidaciones: PRD §14.2.
export function useComprarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: comprarTicket,
    onSuccess: async (t) => {
      qc.setQueryData(['ticket', t.ticketCode], t)
      await Promise.all([
        qc.invalidateQueries({ queryKey: ['tickets', 'usuario', t.userEmail] }),
        qc.invalidateQueries({ queryKey: ['evento', t.eventCode] }),
      ])
    },
  })
}

export const useTicket = (ticketCode: string | undefined) =>
  useQuery({
    queryKey: ['ticket', ticketCode],
    queryFn: () => obtenerTicket(ticketCode ?? ''),
    enabled: Boolean(ticketCode),
    staleTime: 15_000,
  })

/** Consulta bajo demanda (STAFF): solo se ejecuta cuando `codigo` deja de ser null. */
export const useBuscarTicket = (codigo: string | null) =>
  useQuery({
    queryKey: ['ticket', codigo],
    queryFn: () => obtenerTicket(codigo ?? ''),
    enabled: codigo !== null,
    staleTime: 0,
  })

export const useMisTickets = (email: string | undefined) =>
  useQuery({
    queryKey: ['tickets', 'usuario', email],
    queryFn: () => listarTicketsDeUsuario(email ?? ''),
    enabled: Boolean(email),
    staleTime: 15_000,
  })

export const useTicketsPagadosPorEvento = (eventCode: string | undefined) =>
  useQuery({
    queryKey: ['tickets', 'pagados', eventCode],
    queryFn: () => listarTicketsPagadosPorEvento(eventCode ?? ''),
    enabled: Boolean(eventCode),
    staleTime: 15_000,
  })

export function useCancelarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: cancelarTicket,
    onSuccess: async (t) => {
      qc.setQueryData(['ticket', t.ticketCode], t)
      await Promise.all([
        qc.invalidateQueries({ queryKey: ['tickets', 'usuario', t.userEmail] }),
        qc.invalidateQueries({ queryKey: ['tickets', 'pagados', t.eventCode] }),
      ])
    },
  })
}

export function useUsarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: usarTicket,
    onSuccess: async (t) => {
      qc.setQueryData(['ticket', t.ticketCode], t)
      await qc.invalidateQueries({ queryKey: ['tickets', 'pagados', t.eventCode] })
    },
  })
}
