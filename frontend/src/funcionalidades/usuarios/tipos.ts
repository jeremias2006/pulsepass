/**
 * Esquemas Zod de los usuarios: el perfil que devuelve el backend y el formulario de registro.
 *
 * <p>El backend aplana el perfil en la respuesta (los campos firstName, lastName, phone, city y
 * birthDate viven en la entidad UserProfile, pero MapStruct los sube al mismo nivel con
 * `@Mapping(source = "profile.firstName")`). Por eso aquí aparecen todos juntos.</p>
 */
import { z } from 'zod'
import { hoyISO } from '../../lib/formato'

/** dto/response/UserResponse (sin contraseña ni hash). */
export const UsuarioSchema = z.object({
  id: z.number(),
  username: z.string(),
  email: z.string(),
  active: z.boolean(),
  firstName: z.string(),
  lastName: z.string(),
  phone: z.string().nullish(),
  city: z.string().nullish(),
  birthDate: z.string().nullish(),
})
export type Usuario = z.infer<typeof UsuarioSchema>

/**
 * Validación ESTRUCTURAL (PRD §11). La edad mínima de un evento es regla del backend, no de la UI.
 *
 * <p>La distinción es la misma que en el backend entre Bean Validation y las reglas BR: aquí solo
 * se comprueba la FORMA (obligatorio, longitud, formato de correo, fecha no futura). Si se
 * copiara además la edad mínima, habría que replicar en el frontend una regla que depende del
 * evento y que el backend evalúa en la fecha del evento, no en la de hoy.</p>
 */
export const RegistroSchema = z.object({
  username: z
    .string()
    .trim()
    .min(1, 'Ingresa un nombre de usuario')
    .max(50, 'Máximo 50 caracteres'),
  email: z.email('Correo no válido').max(150, 'Máximo 150 caracteres'),
  firstName: z.string().trim().min(1, 'Ingresa tus nombres').max(100, 'Máximo 100 caracteres'),
  lastName: z.string().trim().min(1, 'Ingresa tus apellidos').max(100, 'Máximo 100 caracteres'),
  birthDate: z
    .string()
    .min(1, 'Ingresa tu fecha de nacimiento')
    .refine((v) => v <= hoyISO(), 'La fecha no puede ser futura'),
  password: z
    .string()
    .min(12, 'Mínimo 12 caracteres')
    // El backend limita a 72 BYTES (límite de BCrypt), no a 72 caracteres.
    .refine((v) => new TextEncoder().encode(v).length <= 72, 'Máximo 72 bytes (UTF-8)'),
})
export type RegistroForm = z.infer<typeof RegistroSchema>
