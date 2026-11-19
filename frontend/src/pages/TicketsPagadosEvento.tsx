import { useState, type FormEvent } from 'react'
import BotonCopiar from '../componentes/BotonCopiar'
import { useNavigate, useParams } from 'react-router'
import Aviso from '../componentes/Aviso'
import { BadgeEstadoTicket } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Cargando from '../componentes/Cargando'
import CodigoTexto from '../componentes/CodigoTexto'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import PrecioCOP from '../componentes/PrecioCOP'
import Tabla, { type Columna } from '../componentes/Tabla'
import { ETIQUETA_TIPO } from '../funcionalidades/tickets/etiquetas'
import { useTicketsPagadosPorEvento, useUsarTicket } from '../funcionalidades/tickets/hooks'
import type { Ticket } from '../funcionalidades/tickets/tipos'
import { esNoDisponible, mensajeDeError } from '../lib/errores'
import { RUTAS } from '../lib/rutas'

/**
 * Tickets pagados de un evento (ruta `/staff/eventos/:eventCode/tickets`). Caso de uso: ver el
 * listado de asistentes esperados y validar desde ahí.
 *
 * <p><b>Acceso:</b> rol STAFF o ADMIN. <b>Consume:</b> GET /events/{code}/tickets/paid y
 * PATCH /tickets/{code}/use.</p>
 *
 * <p>Solo lista los PAID: los cancelados no entran y los ya usados no vuelven a entrar. Es, en
 * la práctica, la lista de control de la puerta.</p>
 *
 * <p>Usa `Tabla`, que en móvil convierte cada fila en una tarjeta: esta pantalla se usa de pie,
 * en la entrada del recinto y casi siempre desde un teléfono.</p>
 */
export default function TicketsPagadosEvento() {
  const { eventCode } = useParams()
  const navigate = useNavigate()
  const [texto, setTexto] = useState(eventCode ?? '')
  const q = useTicketsPagadosPorEvento(eventCode)
  const usar = useUsarTicket()

  const ir = (e: FormEvent) => {
    e.preventDefault()
    const c = texto.trim()
    if (c) void navigate(RUTAS.staffTicketsPagados(c))
  }

  const columnas: Columna<Ticket>[] = [
    {
      encabezado: 'Código',
      celda: (t) => (
        <span className="flex items-center gap-1">
          <CodigoTexto>{t.ticketCode}</CodigoTexto>
          <BotonCopiar valor={t.ticketCode} etiqueta="código del ticket" />
        </span>
      ),
    },
    { encabezado: 'Tipo', celda: (t) => ETIQUETA_TIPO[t.type] },
    { encabezado: 'Titular', celda: (t) => t.userEmail },
    { encabezado: 'Precio', celda: (t) => <PrecioCOP valor={t.price} /> },
    { encabezado: 'Estado', celda: (t) => <BadgeEstadoTicket estado={t.status} /> },
    {
      encabezado: 'Acción',
      celda: (t) => (
        <Boton
          variante="secundario"
          cargando={usar.isPending && usar.variables === t.ticketCode}
          onClick={() => usar.mutate(t.ticketCode)}
        >
          Usar
        </Boton>
      ),
    },
  ]

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center gap-2">
        <h1 className="font-titulo text-2xl font-bold">Tickets pagados · {eventCode}</h1>
        {eventCode && <BotonCopiar valor={eventCode} etiqueta="código del evento" />}
      </div>
      <form onSubmit={ir} className="flex items-end gap-3">
        <div className="flex-1">
          <Campo etiqueta="Evento" value={texto} onChange={(e) => setTexto(e.target.value)} />
        </div>
        <Boton type="submit">Ir</Boton>
      </form>

      {usar.isError && <Aviso tono="advertencia">{mensajeDeError(usar.error)}</Aviso>}
      {q.isPending && <Cargando />}
      {q.isError &&
        (esNoDisponible(q.error) ? (
          <EstadoVacio mensaje="Evento no encontrado" />
        ) : (
          <EstadoError error={q.error} onReintentar={() => void q.refetch()} />
        ))}
      {q.data &&
        (q.data.length === 0 ? (
          <EstadoVacio mensaje="No hay tickets pagados" />
        ) : (
          <>
            <p aria-live="polite" className="text-sm text-slate-600 dark:text-slate-400">
              Total: {q.data.length}
            </p>
            <Tabla columnas={columnas} filas={q.data} clave={(t) => t.ticketCode} />
          </>
        ))}
    </div>
  )
}
