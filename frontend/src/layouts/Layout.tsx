/**
 * Marco visual de todas las páginas: barra de navegación, contenido y pie.
 *
 * <p>Es la ruta raíz de `router.tsx`, así que todo lo demás se pinta dentro de su `<Outlet />`.
 * Al estar siempre montado, la barra y el pie no se vuelven a dibujar al cambiar de pantalla.</p>
 *
 * <p>El ancho de la columna no lo decide cada página sino el enrutado: Layout lo lee de los
 * `handle` de las rutas con `useMatches`. Así una pantalla no necesita saber nada de su propia
 * presentación.</p>
 */
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
  // useMatches devuelve todas las rutas que coinciden con la URL actual, de la raíz a la hoja.
  // Se recorren sus `handle` para saber si alguna pidió columna estrecha o ancho completo.
  const handles = useMatches().map((m) => m.handle as HandleRuta | undefined)
  const estrecho = handles.some((h) => h?.estrecho === true)
  const completo = handles.some((h) => h?.completo === true)
  return (
    <div className="flex min-h-screen flex-col bg-slate-50 font-cuerpo text-slate-900 dark:bg-slate-950 dark:text-slate-100">
      {/* Enlace de salto: invisible hasta que recibe el foco con el tabulador. Permite a quien
          navega con teclado o lector de pantalla saltarse la barra de navegación y llegar
          directamente al contenido, en vez de recorrer todos los enlaces en cada página. */}
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
