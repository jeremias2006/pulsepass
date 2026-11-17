import { Link } from 'react-router'
import { RUTAS } from '../../../lib/rutas'
import type { Artista } from '../tipos'

const iniciales = (nombre: string) =>
  nombre
    .split(/\s+/)
    .slice(0, 2)
    .map((p) => p.charAt(0).toUpperCase())
    .join('')

// Colores pastel por artista (mapa completo, sin clases dinámicas); se elige con el id.
const PASTELES = [
  'bg-indigo-100 text-indigo-800 dark:bg-indigo-900 dark:text-indigo-100',
  'bg-emerald-100 text-emerald-800 dark:bg-emerald-900 dark:text-emerald-100',
  'bg-sky-100 text-sky-800 dark:bg-sky-900 dark:text-sky-100',
  'bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-100',
  'bg-rose-100 text-rose-800 dark:bg-rose-900 dark:text-rose-100',
  'bg-violet-100 text-violet-800 dark:bg-violet-900 dark:text-violet-100',
] as const

/** La API no entrega fotos: el avatar muestra las iniciales del nombre artístico. */
export default function AvatarArtista({
  artista,
  orientacion = 'vertical',
}: {
  artista: Artista
  orientacion?: 'vertical' | 'horizontal'
}) {
  const horizontal = orientacion === 'horizontal'
  return (
    <Link
      to={RUTAS.artista(artista.id)}
      className={
        horizontal
          ? 'flex items-center gap-3 rounded-full pr-4 hover:bg-white dark:hover:bg-slate-900'
          : 'flex flex-col items-center gap-2 rounded-xl p-3 text-center hover:bg-white dark:hover:bg-slate-900'
      }
    >
      <span
        aria-hidden="true"
        className={`grid size-16 shrink-0 place-items-center rounded-full font-titulo text-lg font-bold ${PASTELES[(artista.id - 1) % PASTELES.length]}`}
      >
        {iniciales(artista.stageName)}
      </span>
      <span className="text-sm font-medium">{artista.stageName}</span>
    </Link>
  )
}
