import clsx from 'clsx'
import { Link } from 'react-router'
import { BadgeEstadoEvento } from '../../../componentes/Badge'
import { IconoCalendario, IconoUbicacion } from '../../../componentes/iconos'
import { formatearFecha } from '../../../lib/formato'
import { RUTAS } from '../../../lib/rutas'
import { CATEGORIAS } from '../categorias'
import type { EventoResumen } from '../tipos'

export default function TarjetaEvento({ evento }: { evento: EventoResumen }) {
  const cat = CATEGORIAS[evento.category]
  return (
    <Link
      to={RUTAS.evento(evento.eventCode)}
      className="block overflow-hidden rounded-xl bg-white shadow-sm transition-shadow hover:shadow-md focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:bg-slate-900"
    >
      <div className={clsx('flex h-28 items-center justify-center', cat.fondo, cat.texto)}>
        <cat.Icono className="size-10" />
      </div>
      <div className="space-y-2 p-4">
        <p className={clsx('text-xs font-semibold tracking-wide uppercase', cat.etiquetaTexto)}>
          {cat.etiqueta}
        </p>
        <h3 className="font-titulo text-lg font-semibold">{evento.name}</h3>
        <p className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
          <IconoCalendario /> {formatearFecha(evento.eventDate)}
        </p>
        <p className="flex items-center gap-2 text-sm text-slate-600 dark:text-slate-400">
          <IconoUbicacion /> {evento.venueName}
        </p>
        <BadgeEstadoEvento estado={evento.status} />
      </div>
    </Link>
  )
}
