import { http } from '../../lib/http'
import { AuthResponseSchema, UsuarioAutenticadoSchema, type LoginForm } from './tipos'

// ignorar401: en el login un 401 son "credenciales inválidas", no una sesión expirada.
export const login = (datos: LoginForm) =>
  http('/auth/login', AuthResponseSchema, { method: 'POST', body: datos, ignorar401: true })

export const obtenerMe = () => http('/auth/me', UsuarioAutenticadoSchema)
