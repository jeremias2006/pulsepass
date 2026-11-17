import { http } from '../../lib/http'
import { UsuarioSchema, type RegistroForm } from './tipos'

export const registrarUsuario = (datos: RegistroForm) =>
  http('/users', UsuarioSchema, { method: 'POST', body: datos })
export const obtenerUsuarioPorEmail = (email: string) =>
  http(`/users/by-email?email=${encodeURIComponent(email)}`, UsuarioSchema)
export const obtenerUsuarioPorUsername = (username: string) =>
  http(`/users/by-username?username=${encodeURIComponent(username)}`, UsuarioSchema)
