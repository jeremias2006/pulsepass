/**
 * Tipos de la sesión: los tres roles, los DTO de autenticación y la forma de la sesión.
 *
 * <p>Los nombres de rol coinciden con el enum Role del backend. Allí se convierten en autoridades
 * con prefijo (`ROLE_ADMIN`) dentro de SecurityPrincipal, pero esa transformación es interna de
 * Spring Security: por la red viajan tal cual, como los declara `RolSchema`.</p>
 */
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

/**
 * La sesión como unión discriminada: tres estados posibles y ni uno más.
 *
 * <p>Modelarla así (y no con un objeto de banderas sueltas tipo `cargando: boolean`) hace
 * imposibles los estados contradictorios, y TypeScript obliga a tratar los tres casos. El
 * estado 'cargando' es imprescindible: al recargar la pestaña hay un instante en que se está
 * confirmando el token contra /auth/me, y sin él los guardias de ruta tomarían al usuario por
 * anónimo y lo echarían al login en cada F5.</p>
 *
 * <p>Solo el caso 'autenticado' tiene token y usuario, así que el compilador impide leerlos
 * antes de comprobar el estado.</p>
 */
export type Sesion =
  | { estado: 'cargando' }
  | { estado: 'anonimo' }
  | { estado: 'autenticado'; token: string; expiraEn: number; usuario: UsuarioAutenticado }

export const LoginSchema = z.object({
  identifier: z.string().trim().min(1, 'Ingresa tu usuario o correo'),
  password: z.string().min(1, 'Ingresa tu contraseña'),
})
export type LoginForm = z.infer<typeof LoginSchema>

/**
 * Lo único que se guarda en sessionStorage (se valida al leerlo).
 *
 * <p>Se guarda el token y su vencimiento, nada más: ni el correo, ni los roles, ni el perfil.
 * Los roles se vuelven a pedir a /auth/me al restaurar la sesión, de modo que un cambio de
 * permisos surte efecto de inmediato y nadie puede concederse un rol editando el almacenamiento
 * del navegador.</p>
 *
 * <p>Es `sessionStorage` y no `localStorage`: el token desaparece al cerrar la pestaña, lo que
 * acota el riesgo en un equipo compartido. Y se valida con Zod al leerlo porque su contenido es
 * editable por quien use el navegador: es un dato externo más.</p>
 */
export const TokenGuardadoSchema = z.object({ token: z.string().min(1), expiraEn: z.number() })

export const CLAVE_SESION = 'pulsepass.sesion'
export const CLAVE_AVISO = 'pulsepass.aviso'
