import { Navigate, Outlet, useLocation } from 'react-router'
import Cargando from '../componentes/Cargando'
import { useAuth } from '../funcionalidades/auth/hooks'
import { RUTAS } from '../lib/rutas'

/**
 * Guardia de navegación: exige que haya sesión, sin importar el rol.
 *
 * <p>Se usa como ruta padre en `router.tsx`: todas las rutas anidadas dentro heredan la
 * condición, y `<Outlet />` es el hueco donde se pinta la ruta hija cuando se cumple.</p>
 *
 * <p><b>Esto no es seguridad, es navegación.</b> Solo evita que alguien llegue a una pantalla
 * que no podría usar. Quien protege los datos es el backend: `anyRequest().authenticated()` en
 * SecurityConfig responde 401 a cualquier petición sin token válido.</p>
 */
/** Exige sesión. Anónimo → /login guardando en `state.desde` la ruta pedida. */
export default function RutaProtegida() {
  const { sesion } = useAuth()
  const location = useLocation()
  // El estado 'cargando' es imprescindible: al recargar la pestaña hay un instante en que el
  // token se está confirmando contra /auth/me. Sin esta línea, el usuario parecería anónimo y
  // acabaría en el login en cada F5.
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo')
    // `replace` sustituye la entrada del historial en vez de añadir una: así, al pulsar "atrás"
    // desde el login no se vuelve a la ruta protegida, que redirigiría otra vez en bucle.
    // `state.desde` recuerda a dónde iba el usuario, para devolverlo allí tras iniciar sesión.
    return (
      <Navigate to={RUTAS.login} replace state={{ desde: location.pathname + location.search }} />
    )
  return <Outlet />
}
