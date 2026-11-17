import { z } from 'zod'

export const RolSchema = z.enum(['USER', 'STAFF', 'ADMIN'])
export type Rol = z.infer<typeof RolSchema>

/** dto/response/AuthResponse */
export const AuthResponseSchema = z.object({
  accessToken: z.string(),
  tokenType: z.string(),
  expiresIn: z.number(), // segundos (900)
})

/** dto/response/AuthenticatedUserResponse: roles viene como ["USER"], ["ADMIN"]… */
export const UsuarioAutenticadoSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  roles: z.array(RolSchema),
})
export type UsuarioAutenticado = z.infer<typeof UsuarioAutenticadoSchema>

export type Sesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; token: string; expiraEn: number; usuario: UsuarioAutenticado }

export const LoginSchema = z.object({
  identifier: z.string().trim().min(1, 'Ingresa tu usuario o correo'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})
export type LoginForm = z.infer<typeof LoginSchema>

/** Lo único que se guarda en sessionStorage (se valida al leerlo). */
export const TokenGuardadoSchema = z.object({ token: z.string().min(1), expiraEn: z.number() })

export const CLAVE_SESION = 'pulsepass.sesion'
export const CLAVE_AVISO = 'pulsepass.aviso'
