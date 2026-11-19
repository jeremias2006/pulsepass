/**
 * Pie de página: aviso del proyecto y accesos secundarios a sedes y artistas.
 *
 * <p>Son enlaces públicos que no ocupan sitio en la barra principal, reservada a los caminos
 * más usados. Se usa `<Link>` y no `<a>` para que la navegación ocurra dentro de la aplicación,
 * sin recargar la página.</p>
 */
import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function Footer() {
  return (
    <footer className="border-t border-slate-200 py-6 text-sm text-slate-600 dark:border-slate-800 dark:text-slate-400">
      <div className="mx-auto flex max-w-6xl flex-wrap justify-between gap-2 px-4">
        <p>© 2026 PulsePass · Caso de estudio académico</p>
        <p className="flex gap-4">
          <Link to={RUTAS.sedes} className="hover:underline">
            Sedes
          </Link>
          <Link to={RUTAS.artistas} className="hover:underline">
            Artistas
          </Link>
        </p>
      </div>
    </footer>
  )
}
