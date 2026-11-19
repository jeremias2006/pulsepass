import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import {
  cancelarTicket,
  comprarTicket,
  listarTicketsDeUsuario,
  listarTicketsPagadosPorEvento,
  obtenerTicket,
  usarTicket,
} from './api'

/**
 * Hooks de tickets: la funcionalidad con más escrituras, y por tanto con más invalidaciones.
 *
 * <p>Regla general de este archivo: después de cada escritura hay que decirle a la caché qué
 * dejó de ser cierto. Si no se hiciera, la pantalla "Mis tickets" seguiría mostrando el estado
 * anterior hasta que el usuario recargara a mano.</p>
 *
 * <p>Los `staleTime` son más cortos que en eventos (15 segundos) porque estos datos cambian con
 * las acciones del propio usuario y del personal en la puerta.</p>
 */

// Invalidaciones: PRD §14.2.
/**
 * Comprar una entrada (rol USER). Tras el éxito hay que refrescar dos cosas:
 * la lista de tickets del comprador, y el evento, porque pudo quedar en SOLD_OUT si esa compra
 * ocupó el último cupo (BR-008).
 */
export function useComprarTicket() {
  const qc = useQueryClient()
  return useMutation({
    mutationFn: comprarTicket,
    onSuccess: async (t) => {
      qc.setQueryData(['ticket', t.ticketCode], t)
      // Promise.all lanza las dos invalidaciones a la vez en lugar de una tras otra.
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

/**
 * Consulta bajo demanda (STAFF): solo se ejecuta cuando `codigo` deja de ser null.
 *
 * <p>`staleTime: 0` es deliberado y es lo que lo distingue de los demás hooks: en la puerta del
 * evento el dato debe ser del instante. Un estado cacheado podría dejar pasar dos veces el mismo
 * ticket si otra persona del personal acaba de validarlo.</p>
 */
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

/**
 * Cancelar un ticket propio (BR-010/011/012: solo si está PAID y el evento no ha pasado).
 *
 * <p>Se refrescan la lista del usuario y la lista de pagados del evento, que el personal usa
 * para el control de acceso: un ticket cancelado ya no debe figurar ahí.</p>
 *
 * <p>Nota sobre una limitación conocida del backend: cancelar NO devuelve el evento de SOLD_OUT
 * a PUBLISHED, aunque el cupo quede libre. Por eso aquí no se invalida el evento.</p>
 */
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

/**
 * Marcar el ingreso (STAFF o ADMIN): PAID → USED. El backend revisa primero si está CANCELLED
 * y después si está PAID, para poder dar el mensaje correcto en cada caso.
 *
 * <p>Solo se invalida la lista de pagados: quien valida no está viendo la lista personal del
 * asistente.</p>
 */
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
