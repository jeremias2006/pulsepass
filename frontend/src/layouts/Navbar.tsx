/**
 * Barra de navegación superior. Cambia según quién haya iniciado sesión.
 *
 * <p>Los enlaces se calculan a partir de los roles: un USER ve "Mis tickets", un STAFF ve
 * "Validar ingreso" y un ADMIN ve además "Administrar eventos". Es la misma información que usa
 * `RutaPorRol`, pero aquí sirve para no ofrecer caminos que llevarían a un 403.</p>
 *
 * <p><b>Ocultar un enlace no protege nada.</b> Es cortesía: quien escriba la URL a mano verá la
 * pantalla, y el backend le responderá 403 en cuanto pida datos.</p>
 *
 * <p>Incluye el menú plegable para móvil y el menú de usuario, ambos con manejo de teclado.</p>
 */
import clsx from 'clsx'
import { useState, type KeyboardEvent } from 'react'
import { Link, NavLink, useNavigate } from 'react-router'
import { IconoCerrar, IconoMenu, IconoMusica } from '../componentes/iconos'
import { useAuth } from '../funcionalidades/auth/hooks'
import { RUTAS } from '../lib/rutas'

interface Enlace {
  a: string
  texto: string
}

// NavLink pasa `isActive` a su className: así el enlace de la sección actual se resalta solo,
// sin tener que comparar rutas a mano.
const claseEnlace = ({ isActive }: { isActive: boolean }) =>
  clsx(
    'rounded-lg px-3 py-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-marca-600',
    isActive
      ? 'bg-marca-50 text-marca-700 dark:text-marca-500'
      : 'text-slate-700 hover:bg-slate-100 dark:text-slate-200 dark:hover:bg-slate-800',
  )

/**
 * Tabla 8.4 del PRD: los enlaces salen de RUTAS y del rol (solo UX).
 *
 * <p>Los tres primeros son públicos; el resto se añaden según el rol. Obsérvese que un ADMIN
 * ve también "Validar ingreso", porque `tieneRol('STAFF','ADMIN')` se cumple con cualquiera de
 * los dos, igual que `hasAnyRole` en el backend.</p>
 */
function useEnlaces(): Enlace[] {
  const { tieneRol } = useAuth()
  const enlaces: Enlace[] = [
    { a: RUTAS.eventos, texto: 'Eventos' },
    { a: RUTAS.artistas, texto: 'Artistas' },
    { a: RUTAS.sedes, texto: 'Sedes' },
  ]
  if (tieneRol('USER')) enlaces.push({ a: RUTAS.misTickets, texto: 'Mis tickets' })
  if (tieneRol('STAFF', 'ADMIN'))
    enlaces.push({ a: RUTAS.staffValidacion, texto: 'Validar ingreso' })
  if (tieneRol('ADMIN')) enlaces.push({ a: RUTAS.adminEventos, texto: 'Administrar eventos' })
  return enlaces
}

export default function Navbar() {
  const { sesion, cerrarSesion } = useAuth()
  const navigate = useNavigate()
  const enlaces = useEnlaces()
  const [menuAbierto, setMenuAbierto] = useState(false)
  const [usuarioAbierto, setUsuarioAbierto] = useState(false)

  const cerrarMenus = () => {
    setMenuAbierto(false)
    setUsuarioAbierto(false)
  }
  const alTeclear = (e: KeyboardEvent<HTMLElement>) => {
    if (e.key === 'Escape') cerrarMenus()
  }
  const salir = async () => {
    cerrarMenus()
    cerrarSesion()
    await navigate(RUTAS.inicio)
  }

  const autenticado = sesion.estado === 'autenticado'
  const iniciales = autenticado ? sesion.usuario.username.slice(0, 2).toUpperCase() : ''

  const lista = enlaces.map((e) => (
    <NavLink key={e.a} to={e.a} className={claseEnlace} onClick={cerrarMenus}>
      {e.texto}
    </NavLink>
  ))

  return (
    <header
      onKeyDown={alTeclear}
      className="sticky top-0 z-40 border-b border-slate-200 bg-white/95 backdrop-blur dark:border-slate-800 dark:bg-slate-950/95"
    >
      <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-2">
        <div className="flex items-center gap-6">
          <Link
            to={RUTAS.inicio}
            className="flex items-center gap-2 font-titulo text-lg font-bold text-slate-900 dark:text-white"
          >
            <span className="grid size-8 place-items-center rounded-lg bg-marca-600 text-white">
              <IconoMusica className="size-4" />
            </span>
            PulsePass
          </Link>

          <nav aria-label="Principal" className="hidden items-center gap-1 md:flex">
            {lista}
          </nav>
        </div>

        <div className="flex items-center gap-2">
          {autenticado ? (
            <div className="relative">
              <button
                type="button"
                aria-expanded={usuarioAbierto}
                aria-haspopup="menu"
                onClick={() => setUsuarioAbierto((v) => !v)}
                className="flex min-h-11 items-center gap-2 rounded-lg px-2 text-sm font-medium focus-visible:outline-2 focus-visible:outline-marca-600"
              >
                <span
                  aria-hidden="true"
                  className="grid size-8 place-items-center rounded-full bg-marca-600 text-xs font-bold text-white"
                >
                  {iniciales}
                </span>
                <span className="hidden sm:inline">{sesion.usuario.username}</span>
              </button>
              {usuarioAbierto && (
                <div
                  role="menu"
                  className="absolute right-0 mt-2 w-48 rounded-xl bg-white p-2 shadow-lg dark:bg-slate-900"
                >
                  <Link
                    role="menuitem"
                    to={RUTAS.cuenta}
                    onClick={cerrarMenus}
                    className="block rounded-lg px-3 py-2 text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                  >
                    Mi cuenta
                  </Link>
                  <button
                    role="menuitem"
                    type="button"
                    onClick={() => void salir()}
                    className="block w-full rounded-lg px-3 py-2 text-left text-sm hover:bg-slate-100 dark:hover:bg-slate-800"
                  >
                    Salir
                  </button>
                </div>
              )}
            </div>
          ) : (
            sesion.estado === 'anonimo' && (
              <div className="hidden items-center gap-2 md:flex">
                <Link to={RUTAS.login} className={claseEnlace({ isActive: false })}>
                  Ingresar
                </Link>
                <Link
                  to={RUTAS.registro}
                  className="rounded-lg bg-marca-600 px-4 py-2 text-sm font-semibold text-white hover:bg-marca-700"
                >
                  Registrarse
                </Link>
              </div>
            )
          )}

          <button
            type="button"
            aria-expanded={menuAbierto}
            aria-controls="menu-movil"
            aria-label={menuAbierto ? 'Cerrar menú' : 'Abrir menú'}
            onClick={() => setMenuAbierto((v) => !v)}
            className="grid min-h-11 min-w-11 place-items-center rounded-lg md:hidden"
          >
            {menuAbierto ? <IconoCerrar /> : <IconoMenu />}
          </button>
        </div>
      </div>

      {menuAbierto && (
        <nav
          id="menu-movil"
          aria-label="Principal (móvil)"
          className="flex flex-col gap-1 border-t border-slate-200 px-4 py-3 md:hidden dark:border-slate-800"
        >
          {lista}
          {!autenticado && (
            <>
              <NavLink to={RUTAS.login} className={claseEnlace} onClick={cerrarMenus}>
                Ingresar
              </NavLink>
              <NavLink to={RUTAS.registro} className={claseEnlace} onClick={cerrarMenus}>
                Registrarse
              </NavLink>
            </>
          )}
        </nav>
      )}
    </header>
  )
}
