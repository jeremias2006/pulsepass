import { Link, useNavigate } from 'react-router'
import { BadgeNeutro } from '../componentes/Badge'
import Boton from '../componentes/Boton'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import Tarjeta from '../componentes/Tarjeta'
import { useAuth } from '../funcionalidades/auth/hooks'
import { useUsuarioPorEmail } from '../funcionalidades/usuarios/hooks'
import { formatearFecha } from '../lib/formato'
import { RUTAS } from '../lib/rutas'

/**
 * Mi cuenta (ruta `/cuenta`). Caso de uso: ver los datos propios y cerrar sesión.
 *
 * <p><b>Acceso:</b> cualquier sesión, sin importar el rol (dentro de `RutaProtegida`).<br>
 * <b>Consume:</b> GET /users/by-email, donde `@PreAuthorize` solo deja consultar el perfil
 * propio (o cualquiera si eres ADMIN).</p>
 *
 * <p>Los datos llegan de dos sitios: los básicos y los roles están ya en la sesión (vienen de
 * /auth/me), y el perfil completo (nombre, teléfono, ciudad, fecha de nacimiento) se pide
 * aparte, porque /auth/me no lo incluye.</p>
 */
export default function Cuenta() {
  const { sesion, cerrarSesion, tieneRol } = useAuth()
  const navigate = useNavigate()
  // El correo sale SIEMPRE de la sesión, nunca de un campo ni de la URL: pedir el perfil de
  // otra persona devolvería 403 de todos modos, pero conviene no construir siquiera la petición.
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
  const perfil = useUsuarioPorEmail(email) // solo se consulta la identidad propia

  if (sesion.estado !== 'autenticado') return <Cargando />
  const u = sesion.usuario

  const salir = async () => {
    cerrarSesion()
    await navigate(RUTAS.inicio)
  }

  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-2xl font-bold">Mi cuenta</h1>
      <Tarjeta className="space-y-3">
        <p className="flex flex-wrap items-center gap-2">
          <span className="font-titulo text-lg font-semibold">
            {perfil.data ? `${perfil.data.firstName} ${perfil.data.lastName}` : `@${u.username}`}
          </span>
          {u.roles.map((r) => (
            <BadgeNeutro key={r}>{r}</BadgeNeutro>
          ))}
        </p>
        <p className="text-slate-600 dark:text-slate-400">
          @{u.username} · {u.email}
        </p>
        {perfil.isPending && <Cargando />}
        {perfil.isError && (
          <EstadoError error={perfil.error} onReintentar={() => void perfil.refetch()} />
        )}
        {perfil.data && (
          <p className="text-sm text-slate-600 dark:text-slate-400">
            {perfil.data.birthDate && <>Nacimiento: {formatearFecha(perfil.data.birthDate)} · </>}
            Estado: {perfil.data.active ? 'Activa' : 'Inactiva'}
          </p>
        )}
      </Tarjeta>
      <div className="flex flex-wrap gap-3">
        {tieneRol('USER') && (
          <Link
            to={RUTAS.misTickets}
            className="inline-flex min-h-11 items-center rounded-lg border border-slate-300 px-4 font-semibold"
          >
            Ver mis tickets
          </Link>
        )}
        <Boton variante="secundario" onClick={() => void salir()}>
          Cerrar sesión
        </Boton>
      </div>
    </div>
  )
}
