import { useState } from 'react'
import BotonCopiar from '../componentes/BotonCopiar'
import { Link, useLocation, useParams } from 'react-router'
import Aviso from '../componentes/Aviso'
import { BadgeEstadoEvento } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import Cargando from '../componentes/Cargando'
import CodigoTexto from '../componentes/CodigoTexto'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import Modal from '../componentes/Modal'
import Selector from '../componentes/Selector'
import Tarjeta from '../componentes/Tarjeta'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { CATEGORIAS } from '../funcionalidades/eventos/categorias'
import { useAsociarArtista, useEvento, usePublicarEvento } from '../funcionalidades/eventos/hooks'
import { esNoDisponible, mensajeDeError } from '../lib/errores'
import { formatearFecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

const recienCreado = (state: unknown): boolean =>
  typeof state === 'object' && state !== null && 'creado' in state && state.creado === true

export default function AdminGestionEvento() {
  const { eventCode = '' } = useParams()
  const location = useLocation()
  const evento = useEvento(eventCode)
  const artistas = useArtistasActivos()
  const publicar = usePublicarEvento()
  const asociar = useAsociarArtista(eventCode)
  const [confirmando, setConfirmando] = useState(false)
  const [artistaId, setArtistaId] = useState('')
  const [avisoCreado, setAvisoCreado] = useState(() => recienCreado(location.state))

  if (evento.isPending) return <Cargando />
  if (evento.isError)
    return esNoDisponible(evento.error) ? (
      <EstadoVacio
        mensaje="Evento no disponible"
        accion={
          <Link to={RUTAS.adminEventos} className="font-medium text-marca-700 dark:text-marca-500">
            Volver a eventos
          </Link>
        }
      />
    ) : (
      <EstadoError error={evento.error} onReintentar={() => void evento.refetch()} />
    )

  const ev = evento.data
  // Solo UX: la UI oculta los ya asociados, pero el backend sigue siendo la autoridad (409 si se repite).
  const disponibles = (artistas.data ?? []).filter((a) => !ev.artists.some((x) => x.id === a.id))

  return (
    <div className="space-y-6">
      <Link
        to={RUTAS.adminEventos}
        className="text-sm font-medium text-marca-700 dark:text-marca-500"
      >
        ← Eventos
      </Link>
      {avisoCreado && (
        <Aviso tono="exito" autoCerrarMs={4000} onCerrar={() => setAvisoCreado(false)}>
          Evento creado.
        </Aviso>
      )}

      <header className="space-y-1">
        <h1 className="font-titulo text-2xl font-bold">
          {ev.name} · <CodigoTexto>{ev.eventCode}</CodigoTexto>
        </h1>
        <BotonCopiar valor={ev.eventCode} etiqueta="código del evento" />
        <p className="flex flex-wrap items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
          <BadgeEstadoEvento estado={ev.status} /> {CATEGORIAS[ev.category].etiqueta} ·{' '}
          {formatearFecha(ev.eventDate)} · Mín. {ev.minimumAge ?? 0} · {ev.venueName}
        </p>
      </header>

      <Tarjeta className="space-y-3">
        <h2 className="font-titulo text-lg font-semibold">Publicación</h2>
        <p className="text-sm text-slate-600 dark:text-slate-400">
          El evento es visible al público una vez publicado.
        </p>
        {publicar.isSuccess && (
          <Aviso tono="exito" autoCerrarMs={4000} onCerrar={() => publicar.reset()}>
            Evento publicado.
          </Aviso>
        )}
        {publicar.isError && <Aviso tono="error">{mensajeDeError(publicar.error)}</Aviso>}
        <Boton disabled={ev.status !== 'DRAFT'} onClick={() => setConfirmando(true)}>
          Publicar evento
        </Boton>
      </Tarjeta>

      <Tarjeta className="space-y-3">
        <h2 className="font-titulo text-lg font-semibold">Artistas asociados</h2>
        {ev.artists.length === 0 ? (
          <p className="text-sm text-slate-600 dark:text-slate-400">
            Aún no hay artistas asociados.
          </p>
        ) : (
          <ul className="list-inside list-disc text-sm">
            {ev.artists.map((a) => (
              <li key={a.id}>{a.stageName}</li>
            ))}
          </ul>
        )}
        {asociar.isError && <Aviso tono="error">{mensajeDeError(asociar.error)}</Aviso>}
        <div className="flex items-end gap-3">
          <div className="flex-1">
            <Selector
              etiqueta="Agregar artista"
              value={artistaId}
              onChange={(e) => setArtistaId(e.target.value)}
            >
              <option value="">Selecciona un artista…</option>
              {disponibles.map((a) => (
                <option key={a.id} value={a.id}>
                  {a.stageName}
                </option>
              ))}
            </Selector>
          </div>
          <Boton
            disabled={!artistaId}
            cargando={asociar.isPending}
            onClick={() => asociar.mutate(Number(artistaId), { onSuccess: () => setArtistaId('') })}
          >
            Asociar
          </Boton>
        </div>
      </Tarjeta>

      <p>
        <Link
          to={RUTAS.staffTicketsPagados(ev.eventCode)}
          className="font-medium text-marca-700 dark:text-marca-500"
        >
          Tickets pagados →
        </Link>
      </p>

      <Modal abierto={confirmando} titulo="Publicar evento" onCerrar={() => setConfirmando(false)}>
        <p className="mb-4 text-sm">
          ¿Publicar <strong>{ev.name}</strong>? Será visible para todo el público.
        </p>
        <div className="flex justify-end gap-3">
          <Boton variante="secundario" onClick={() => setConfirmando(false)}>
            Cancelar
          </Boton>
          <Boton
            cargando={publicar.isPending}
            onClick={() =>
              publicar.mutate(ev.eventCode, { onSettled: () => setConfirmando(false) })
            }
          >
            Publicar
          </Boton>
        </div>
      </Modal>
    </div>
  )
}
