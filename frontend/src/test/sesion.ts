import { http, HttpResponse } from 'msw'
import { CLAVE_SESION, type Rol } from '../funcionalidades/auth/tipos'
import { server } from './server'

/** Deja una sesión restaurable en sessionStorage y hace que /auth/me devuelva ese usuario y esos roles. */
export function iniciarComo(roles: Rol[], email = 'andrea@pulsepass.test', username = 'andrea') {
  sessionStorage.setItem(
    CLAVE_SESION,
    JSON.stringify({ token: 'tok', expiraEn: Date.now() + 60_000 }),
  )
  server.use(http.get('*/api/auth/me', () => HttpResponse.json({ id: 2, username, email, roles })))
}
