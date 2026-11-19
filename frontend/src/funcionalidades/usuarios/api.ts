/**
 * Llamadas al backend para los usuarios: registro y consulta del perfil propio.
 *
 * <p>Nótese que el registro es `POST /users` y no una ruta de /auth: lo atiende UserController,
 * que delega en AuthService para crear en una sola transacción el usuario, su perfil y su
 * credencial con la contraseña ya cifrada en BCrypt. El rol USER se fija en el servidor, así que
 * desde aquí es imposible registrarse como STAFF o ADMIN.</p>
 */
import { http } from '../../lib/http'
import { UsuarioSchema, type RegistroForm } from './tipos'

// Público (permitAll). La respuesta no incluye contraseña ni hash.
export const registrarUsuario = (datos: RegistroForm) =>
  http('/users', UsuarioSchema, { method: 'POST', body: datos })
// Exige sesión; @PreAuthorize solo deja consultar el propio perfil (o cualquiera si eres ADMIN).
export const obtenerUsuarioPorEmail = (email: string) =>
  http(`/users/by-email?email=${encodeURIComponent(email)}`, UsuarioSchema)
export const obtenerUsuarioPorUsername = (username: string) =>
  http(`/users/by-username?username=${encodeURIComponent(username)}`, UsuarioSchema)
