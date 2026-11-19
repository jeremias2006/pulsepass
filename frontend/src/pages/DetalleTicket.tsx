import { Link, useParams } from 'react-router'
import BotonCopiar from '../componentes/BotonCopiar'
import { BadgeEstadoTicket } from '../componentes/Badge'
import Cargando from '../componentes/Cargando'
import CodigoTexto from '../componentes/CodigoTexto'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import PrecioCOP from '../componentes/PrecioCOP'
import Tarjeta from '../componentes/Tarjeta'
import CancelarTicket from '../funcionalidades/tickets/componentes/CancelarTicket'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/etiquetas'
import { useTicket } from '../funcionalidades/tickets/hooks'
import { esNoDisponible } from '../lib/errores'
import { formatearFechaHora } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

/**
 * Detalle de un ticket (ruta `/mis-tickets/:ticketCode`). Caso de uso: ver una entrada completa
 * y, si procede, cancelarla.
 *
 * <p><b>Acceso:</b> rol USER, y además el backend comprueba la propiedad con
 * `@PreAuthorize(... isTicketOwner ...)`.<br>
 * <b>Consume:</b> GET /tickets/{code}.</p>
 *
 * <p>Muestra el código en fuente monoespaciada y con botón de copiar, porque es el dato que se
 * presenta en la puerta del evento.</p>
 */
export default function DetalleTicket() {
  const { ticketCode } = useParams()
  const q = useTicket(ticketCode)

  if (q.isPending) return <Cargando />
  if (q.isError) {
    // 403 (ajeno) y 404 (inexistente) se ven igual: no se revela si el código existe.
    // Es la contraparte de la política anti-enumeración del backend: si esta pantalla
    // distinguiera los dos casos, delataría justo lo que allí se oculta. De ahí `esNoDisponible`.
    if (esNoDisponible(q.error))
      return (
        <EstadoVacio
          mensaje="Ticket no disponible"
          accion={
            <Link to={RUTAS.misTickets} className="font-medium text-marca-700 dark:text-marca-500">
              Volver a mis tickets
            </Link>
          }
        />
      )
    return <EstadoError error={q.error} onReintentar={() => void q.refetch()} />
  }

  const t = q.data
  return (
    <div className="space-y-4">
      <Link
        to={RUTAS.misTickets}
        className="text-sm font-medium text-marca-700 dark:text-marca-500"
      >
        ← Mis tickets
      </Link>
      <Tarjeta className="space-y-3">
        <div className="flex items-start justify-between gap-3">
          <h1 className="font-titulo text-xl font-bold">{t.eventName}</h1>
          <BadgeEstadoTicket estado={t.status} />
        </div>
        <dl className="space-y-2 text-sm">
          <div className="flex items-center justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Código</dt>
            <dd className="flex items-center gap-2">
              <CodigoTexto>{t.ticketCode}</CodigoTexto>
              <BotonCopiar valor={t.ticketCode} etiqueta="código del ticket" />
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Tipo</dt>
            <dd>{ETIQUETA_TIPO[t.type]}</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Precio</dt>
            <dd>
              <PrecioCOP valor={t.price} />
            </dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Comprado el</dt>
            <dd>{formatearFechaHora(t.purchaseDate)}</dd>
          </div>
          <div className="flex justify-between gap-3">
            <dt className="text-slate-500 dark:text-slate-400">Titular</dt>
            <dd>{t.userEmail}</dd>
          </div>
        </dl>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          Presenta este código en el ingreso.
        </p>
      </Tarjeta>
      <CancelarTicket ticket={t} />
    </div>
  )
}
