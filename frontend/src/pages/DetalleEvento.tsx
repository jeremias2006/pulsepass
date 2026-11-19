import { Link, useParams } from 'react-router'
import BotonCopiar from '../componentes/BotonCopiar'
import { BadgeEstadoEvento } from '../componentes/Badge'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import { IconoCalendario } from '../componentes/iconos'
import Tarjeta from '../componentes/Tarjeta'
import AvatarArtista from '../funcionalidades/artistas/componentes/AvatarArtista'
import { useAuth } from '../funcionalidades/auth/hooks'
import { CATEGORIAS } from '../funcionalidades/eventos/categorias'
import { useEvento } from '../funcionalidades/eventos/hooks'
import { useSede } from '../funcionalidades/sedes/hooks'
import PanelCompra from '../funcionalidades/tickets/componentes/PanelCompra'
import { esNoDisponible } from '../lib/errores'
import { formatearFecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

/**
 * Detalle de un evento (ruta `/eventos/:eventCode`). Caso de uso: ver toda la información y
 * comprar la entrada. Es la pantalla central del flujo de compra.
 *
 * <p><b>Acceso:</b> pública a nivel de URL. El backend oculta los borradores: un evento en
 * DRAFT responde 404 a quien no sea ADMIN, de modo que nadie puede descubrir su existencia
 * probando códigos.<br>
 * <b>Consume:</b> GET /events/{code} y GET /venues/{code}.</p>
 *
 * <p>La compra se delega en `PanelCompra`, que decide por su cuenta si mostrarse según el rol y
 * el estado del evento.</p>
 */
export default function DetalleEvento() {
    // useParams lee el trozo variable de la ruta: en /eventos/EVT-001, eventCode vale "EVT-001".
    const { eventCode } = useParams()
    const { tieneRol } = useAuth()
    const evento = useEvento(eventCode)
    // EventResponse solo trae código y nombre de la sede: ciudad y capacidad salen de GET /api/venues/{code}.
    // Son dos consultas ENCADENADAS: esta depende del resultado de la anterior, y por eso recibe
    // `evento.data?.venueCode`, que es undefined hasta que llega el evento. El `enabled` del hook
    // la mantiene en espera mientras tanto.
    const sede = useSede(evento.data?.venueCode)

    if (evento.isPending) return <Cargando />
    if (evento.isError) {
        // GAP-02: un borrador (404 para el público) y un evento inexistente se presentan igual.
        if (esNoDisponible(evento.error))
            return (
                <EstadoVacio
                    mensaje="Evento no disponible"
                    accion={
                        <Link to={RUTAS.eventos} className="font-medium text-marca-700 dark:text-marca-500">
                            Volver a la cartelera
                        </Link>
                    }
                />
            )
        return <EstadoError error={evento.error} onReintentar={() => void evento.refetch()} />
    }

    const ev = evento.data
    const cat = CATEGORIAS[ev.category]
    return (
        // pb-24 deja espacio para la barra de compra fija en móvil (DES-07).
        <div className="space-y-6 pb-24 md:pb-0">
            <Link to={RUTAS.eventos} className="text-sm font-medium text-marca-700 dark:text-marca-500">
                ← Volver a la cartelera
            </Link>
            <div className="grid gap-6 md:grid-cols-[1fr_20rem]">
                <Tarjeta className="space-y-4">
                    <div
                        className={`flex h-40 items-center justify-center rounded-lg ${cat.fondo} ${cat.texto}`}
                    >
                        <cat.Icono className="size-14" />
                    </div>
                    <p className="flex items-center gap-2 text-sm">
                        <span className="font-semibold uppercase">{cat.etiqueta}</span>
                        <BadgeEstadoEvento estado={ev.status} />
                    </p>
                    <h1 className="font-titulo text-2xl font-bold">{ev.name}</h1>
                    {tieneRol('STAFF', 'ADMIN') && (
                        // Atajo de operación (mejora propia): el código es lo que piden /admin/eventos y «Tickets por validar».
                        <p className="flex flex-wrap items-center gap-x-4 gap-y-1 rounded-lg bg-slate-100 px-3 py-2 text-sm dark:bg-slate-800">
              <span>
                Código: <strong>{ev.eventCode}</strong>
              </span>
                            <BotonCopiar valor={ev.eventCode} etiqueta="código del evento" />
                            <Link
                                to={RUTAS.staffTicketsPagados(ev.eventCode)}
                                className="font-semibold text-marca-700 dark:text-marca-500"
                            >
                                Ver tickets pagados →
                            </Link>
                            {tieneRol('ADMIN') && (
                                <Link
                                    to={RUTAS.adminEvento(ev.eventCode)}
                                    className="font-semibold text-marca-700 dark:text-marca-500"
                                >
                                    Gestionar evento →
                                </Link>
                            )}
                        </p>
                    )}
                    <p className="flex items-center gap-2">
                        <IconoCalendario /> {formatearFecha(ev.eventDate)}
                    </p>
                    <p>Edad mínima: {ev.minimumAge ? ev.minimumAge : 'todas las edades'}</p>
                    {ev.description && (
                        <p className="whitespace-pre-line text-slate-700 dark:text-slate-300">
                            {ev.description}
                        </p>
                    )}

                    <section aria-labelledby="art">
                        <h2 id="art" className="font-titulo font-semibold">
                            Artistas
                        </h2>
                        {ev.artists.length === 0 ? (
                            <p className="text-sm text-slate-500 dark:text-slate-400">
                                Aún sin artistas confirmados.
                            </p>
                        ) : (
                            <div className="grid grid-cols-2 gap-2 sm:grid-cols-3">
                                {ev.artists.map((a) => (
                                    <AvatarArtista key={a.id} artista={a} />
                                ))}
                            </div>
                        )}
                    </section>

                    <section aria-labelledby="sede">
                        <h2 id="sede" className="font-titulo font-semibold">
                            Sede
                        </h2>
                        <Link
                            to={RUTAS.sede(ev.venueCode)}
                            className="font-medium text-marca-700 dark:text-marca-500"
                        >
                            {ev.venueName}
                        </Link>
                        {sede.data && (
                            <p className="text-sm text-slate-600 dark:text-slate-400">
                                {sede.data.city} · Capacidad {sede.data.capacity}
                            </p>
                        )}
                    </section>
                </Tarjeta>
                <aside>
                    <PanelCompra evento={ev} />
                </aside>
            </div>
        </div>
    )
}