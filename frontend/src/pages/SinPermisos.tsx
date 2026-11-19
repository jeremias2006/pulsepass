import { Link } from 'react-router'
import { RUTAS } from '../lib/rutas'

/**
 * Página 403 (ruta `/403`). Es el destino de `RutaPorRol` cuando hay sesión pero falta el rol.
 *
 * <p>Se distingue del 404 a propósito, igual que en el backend: 401 es "no sé quién eres" y
 * lleva al login; 403 es "sé quién eres, pero no puedes" y lleva aquí. Mandar al login a quien
 * ya tiene sesión no serviría de nada, porque volver a entrar con la misma cuenta no cambiaría
 * sus permisos.</p>
 */
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
