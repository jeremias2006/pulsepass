import { Link } from 'react-router'
import { BadgeEstadoTicket } from '../../../componentes/Badge'
import CodigoTexto from '../../../componentes/CodigoTexto'
import PrecioCOP from '../../../componentes/PrecioCOP'
import Tarjeta from '../../../componentes/Tarjeta'
import { formatearFecha } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import { ETIQUETA_TIPO } from '../etiquetas'
import type { Ticket } from '../tipos'
import CancelarTicket from './CancelarTicket'

export default function TarjetaTicket({ ticket }: { ticket: Ticket }) {
  return (
    <Tarjeta className="space-y-2">
      <div className="flex items-start justify-between gap-3">
        <h2 className="font-titulo text-lg font-semibold">{ticket.eventName}</h2>
        <BadgeEstadoTicket estado={ticket.status} />
      </div>
      <p className="text-sm text-slate-600 dark:text-slate-400">
        <CodigoTexto>{ticket.ticketCode}</CodigoTexto> · {ETIQUETA_TIPO[ticket.type]} ·{' '}
        <PrecioCOP valor={ticket.price} /> · {formatearFecha(ticket.purchaseDate)}
      </p>
      <div className="flex flex-wrap gap-3 pt-2">
        <Link
          to={RUTAS.ticket(ticket.ticketCode)}
          className="inline-flex min-h-11 items-center rounded-lg border border-slate-300 px-4 text-sm font-semibold"
        >
          Ver detalle
        </Link>
        <CancelarTicket ticket={ticket} />
      </div>
    </Tarjeta>
  )
}
