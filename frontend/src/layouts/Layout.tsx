import clsx from 'clsx'
import { Outlet, useMatches } from 'react-router'
import Footer from './Footer'
import Navbar from './Navbar'

interface HandleRuta {
  estrecho?: boolean
  /** Inicio: el hero va de borde a borde, así que la página define sus propios contenedores. */
  completo?: boolean
}

/** DES-04: las rutas con `handle: { estrecho: true }` usan una columna centrada (max-w-3xl);
 *  con `handle: { completo: true }` la página ocupa todo el ancho (hero de Inicio). */
export default function Layout() {
  const handles = useMatches().map((m) => m.handle as HandleRuta | undefined)
  const estrecho = handles.some((h) => h?.estrecho === true)
  const completo = handles.some((h) => h?.completo === true)
  return (
    <div className="flex min-h-screen flex-col bg-slate-50 font-cuerpo text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      <a
        href="#contenido"
        className="sr-only focus:not-sr-only focus:absolute focus:top-2 focus:left-2 focus:z-50 focus:rounded focus:bg-white focus:p-3"
      >
        Saltar al contenido
      </a>
      <Navbar />
      <main
        id="contenido"
        className={clsx(
          'w-full flex-1',
          !completo && 'mx-auto px-4 py-8',
          !completo && (estrecho ? 'max-w-3xl' : 'max-w-6xl'),
        )}
      >
        <Outlet />
      </main>
      <Footer />
    </div>
  )
}
