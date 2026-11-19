import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

/**
 * Página 404. Cumple dos funciones:
 *
 * <ul>
 *   <li>Como ruta `*` del router, captura cualquier URL que no corresponda a ninguna pantalla.
 *       Va la última, porque el comodín coincide con todo.</li>
 *   <li>Como componente normal, lo usan DetalleArtista y DetalleSede para mostrar el 404 de un
 *       recurso concreto, pasándole su propio `mensaje`.</li>
 * </ul>
 */
/** Ruta `*` y, con `mensaje`, el 404 de un recurso concreto (artista, sede…). */
export default function NoEncontrada({
  mensaje = 'No encontramos esta página',
}: {
  mensaje?: string
}) {
  return (
    <section className="py-16 text-center">
      <p className="font-titulo text-6xl font-bold text-marca-600">404</p>
      <h1 className="mt-4 text-xl font-semibold">{mensaje}</h1>
      <Link
        to={RUTAS.inicio}
        className="mt-6 inline-block rounded-lg bg-marca-600 px-4 py-2 font-semibold text-white"
      >
        Volver al inicio
      </Link>
    </section>
  )
}
