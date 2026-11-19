import { Navigate, Outlet, useLocation } from 'react-router'
import Cargando from '../componentes/Cargando'
import { useAuth } from '../funcionalidades/auth/hooks'
import { desdeSeguro } from '../lib/rutas'

/**
 * Guardia inverso: estas rutas solo tienen sentido SIN sesión.
 *
 * <p>Envuelve a /login y /registro. Si alguien con la sesión abierta escribe esas direcciones,
 * no se le muestra un formulario inútil: se le devuelve a donde estaba.</p>
 *
 * <p>No tiene equivalente en el backend, porque allí nada impide volver a hacer login. Es un
 * detalle de experiencia de uso, no una restricción.</p>
 */
/** /login y /registro: con sesión activa redirigen (a la ruta de origen si existe, si no a "/"). */
export default function SoloAnonimo() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  // Se reutiliza `state.desde` para completar el viaje de ida y vuelta: quien fue enviado al
  // login desde una ruta protegida aterriza de nuevo allí. `desdeSeguro` valida ese destino
  // antes de usarlo, porque viene de fuera (ver lib/rutas).
  if (sesion.estado === 'autenticado') return <Navigate to={desdeSeguro(location.state)} replace />
  return <Outlet />
}
