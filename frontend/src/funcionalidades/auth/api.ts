/**
 * Las dos únicas llamadas de autenticación. El registro NO está aquí: es `POST /users`, en
 * funcionalidades/usuarios/api.ts, porque lo atiende UserController.
 */
import { http } from '../../lib/http'
import { AuthResponseSchema, UsuarioAutenticadoSchema, type LoginForm } from './tipos'

// El identificador puede ser el correo o el nombre de usuario: el backend decide por la presencia
// de "@". Ante cualquier fallo responde siempre el mismo 401 genérico, para que nadie pueda
// averiguar qué cuentas existen.
// ignorar401: en el login un 401 son "credenciales inválidas", no una sesión expirada.
export const login = (datos: LoginForm) =>
  http('/auth/login', AuthResponseSchema, { method: 'POST', body: datos, ignorar401: true })

// Quién soy, según el token que acompaña a esta petición. Es la fuente de los ROLES: el JWT no
// los lleva dentro, el backend los lee de la base de datos en cada petición.
export const obtenerMe = () => http('/auth/me', UsuarioAutenticadoSchema)
