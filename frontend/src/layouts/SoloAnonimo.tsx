import { Navigate, Outlet, useLocation } from 'react-router'
import Cargando from '../componentes/Cargando'
import { useAuth } from '../funcionalidades/auth/hooks'
import { desdeSeguro } from '../lib/rutas'

/** /login y /registro: con sesión activa redirigen (a la ruta de origen si existe, si no a "/"). */
export default function SoloAnonimo() {
  const { sesion } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'autenticado') return <Navigate to={desdeSeguro(location.state)} replace />
  return <Outlet />
}
