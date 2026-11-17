import { Link, useParams } from 'react-router'
import BotonCopiar from '../componentes/BotonCopiar'
import { BadgeEstadoTicket } from '../componentes/Badge'
import Cargando from '../componentes/Cargando'
import CodigoTexto from '../componentes/CodigoTexto'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import { IconoCheck } from '../componentes/iconos'
import PrecioCOP from '../componentes/PrecioCOP'
import Tarjeta from '../componentes/Tarjeta'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/etiquetas'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { esNoDisponible } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

export default function ConfirmacionCompra() {
  const { ticketCode } = useParams()
  const q = useTicket(ticketCode)

  if (q.isPending) return <Cargando />
  if (q.isError)
    return esNoDisponible(q.error) ? (
      <EstadoVacio mensaje="Ticket no disponible" />
    ) : (
      <EstadoError error={q.error} onReintentar={() => void q.refetch()} />
    )

  const t = q.data
  return (
    <Tarjeta className="space-y-4 py-8 text-center">
      <div className="flex justify-center text-emerald-600">
        <IconoCheck className="size-12" />
      </div>
      <h1 className="font-titulo text-2xl font-bold">¡Compra realizada con éxito!</h1>
      <p className="text-lg">{t.eventName}</p>
      <p className="flex flex-wrap items-center justify-center gap-2 text-sm">
        Ticket <CodigoTexto>{t.ticketCode}</CodigoTexto>{' '}
        <BotonCopiar valor={t.ticketCode} etiqueta="código del ticket" /> · {ETIQUETA_TIPO[t.type]}{' '}
        · <PrecioCOP valor={t.price} /> <BadgeEstadoTicket estado={t.status} />
      </p>
      <div className="flex flex-wrap justify-center gap-3 pt-2">
        <Link
          to={RUTAS.misTickets}
          className="inline-flex min-h-11 items-center rounded-lg bg-marca-600 px-4 font-semibold text-white"
        >
          Ver mis tickets
        </Link>
        <Link
          to={RUTAS.eventos}
          className="inline-flex min-h-11 items-center rounded-lg border border-slate-300 px-4 font-semibold"
        >
          Seguir explorando
        </Link>
      </div>
    </Tarjeta>
  )
}
