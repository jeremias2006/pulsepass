import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

export default function SinPermisos() {
  return (
    <section className="py-16 text-center">
      <p className="font-titulo text-6xl font-bold text-marca-600">403</p>
      <h1 className="mt-4 text-xl font-semibold">
        No tienes permisos para acceder a esta sección.
      </h1>
      <Link
        to={RUTAS.inicio}
        className="mt-6 inline-block rounded-lg bg-marca-600 px-4 py-2 font-semibold text-white"
      >
        Volver al inicio
      </Link>
    </section>
  )
}
