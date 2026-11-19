import { Navigate, Outlet, useLocation } from 'react-router'
import Cargando from '../componentes/Cargando'
import { useAuth } from '../funcionalidades/auth/hooks'
import type { Rol } from '../funcionalidades/auth/tipos'
import { RUTAS } from '../lib/rutas'

/**
 * Guardia de navegación por rol: exige sesión Y al menos uno de los roles indicados.
 *
 * <p>Es el espejo de las reglas por URL de SecurityConfig, y se usa igual en `router.tsx`:
 * `<RutaPorRol roles={['STAFF','ADMIN']} />` equivale a `hasAnyRole("STAFF","ADMIN")`.</p>
 *
 * <p>Repárese en los dos destinos distintos, que corresponden a los dos códigos del backend:
 * sin sesión va al login (401, "no sé quién eres") y con sesión pero sin el rol va a /403
 * ("sé quién eres, pero no puedes").</p>
 *
 * <p><b>Tampoco esto es seguridad.</b> Forzar la URL de administración a mano muestra la
 * pantalla, pero todas sus peticiones reciben 403 del backend.</p>
 */
export default function RutaPorRol({ roles }: { roles: Rol[] }) {
  const { sesion, tieneRol } = useAuth()
  const location = useLocation()
  if (sesion.estado === 'cargando') return <Cargando />
  if (sesion.estado === 'anonimo')
    return (
      <Navigate to={RUTAS.login} replace state={{ desde: location.pathname + location.search }} />
    )
  // Autenticado pero sin el rol: a /403. No se le manda al login, porque volver a entrar con la
  // misma cuenta no cambiaría nada.
  if (!tieneRol(...roles)) return <Navigate to={RUTAS.sinPermisos} replace />
  return <Outlet />
}
