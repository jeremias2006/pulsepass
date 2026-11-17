import type { FieldValues, Path, UseFormSetError } from 'react-hook-form'
import { ApiError } from './http'

/**
 * El backend responde los 409/400 en inglés (y algunos incluyen datos internos, p. ej. el correo).
 * Se traducen los mensajes de negocio conocidos; si no hay coincidencia se muestra el original.
 */
const MENSAJES_NEGOCIO: [RegExp, string][] = [
  [/^Only PAID tickets can be used\. Current status: USED/, 'El ticket ya fue utilizado.'],
  [/^Only PAID tickets can be used/, 'Solo se pueden usar tickets pagados.'],
  [/^Cancelled ticket cannot be used/, 'El ticket está cancelado y no se puede usar.'],
  [/^Only PAID tickets can be cancelled/, 'Solo se pueden cancelar tickets pagados.'],
  [
    /^Ticket cannot be cancelled after the event date/,
    'No se puede cancelar un ticket después de la fecha del evento.',
  ],
  [/^User does not meet minimum age/, 'No cumples la edad mínima exigida para este evento.'],
  [/^User has no birth date registered/, 'Tu cuenta no tiene fecha de nacimiento registrada.'],
  [/^Event has no capacity left/, 'No hay cupo disponible para este evento.'],
  [
    /^Tickets can only be purchased for PUBLISHED events/,
    'Este evento no está disponible para la venta.',
  ],
  [/^Event already took place/, 'El evento ya se realizó.'],
  [/^Inactive user cannot purchase/, 'Tu cuenta está inactiva y no puede comprar.'],
  [/^Only DRAFT events can be published/, 'Solo se pueden publicar eventos en borrador.'],
  [/^Event date must be in the future/, 'La fecha del evento debe ser posterior a hoy.'],
  [/^Venue is inactive/, 'La sede está inactiva.'],
  [/^Event code already exists/, 'Ya existe un evento con ese código.'],
  [/^Artist \d+ is already assigned/, 'El artista ya está asociado a este evento.'],
  [
    /^Cannot add artists to an event in status/,
    'No se pueden agregar artistas con el estado actual del evento.',
  ],
  [/^Minimum age cannot be negative/, 'La edad mínima no puede ser negativa.'],
]

export function traducirMensajeNegocio(mensaje: string): string {
  return MENSAJES_NEGOCIO.find(([patron]) => patron.test(mensaje))?.[1] ?? mensaje
}

/** Mensaje seguro para mostrar a la persona según el código HTTP (PRD §13). Siempre texto plano. */
export function mensajeDeError(e: unknown): string {
  if (!(e instanceof ApiError)) return 'Ocurrió un error inesperado. Inténtalo más tarde.'
  switch (e.status) {
    case 0:
      return 'No pudimos conectar con el servidor.'
    case 401:
      return 'Tu sesión expiró. Inicia sesión de nuevo.'
    case 403:
      return 'No tienes permisos para realizar esta acción.'
    case 404:
      return 'No encontramos lo que buscas.'
    case 400:
    case 409:
      return traducirMensajeNegocio(e.message)
    default:
      return 'Ocurrió un error inesperado. Inténtalo más tarde.'
  }
}

/** 403 y 404 se tratan igual ante recursos ajenos o inexistentes: no se revela cuál de los dos es (PRD §9.12). */
export function esNoDisponible(e: unknown): boolean {
  return e instanceof ApiError && (e.status === 403 || e.status === 404)
}

/**
 * 400 con `details` → un error por campo (setError). Devuelve los mensajes cuya clave no
 * corresponde a un campo del formulario, para mostrarlos como aviso general.
 */
export function aplicarErroresServidor<T extends FieldValues>(
  e: unknown,
  setError: UseFormSetError<T>,
  campos: readonly string[],
): string[] {
  if (!(e instanceof ApiError) || e.status !== 400) return []
  const generales: string[] = []
  for (const [campo, mensaje] of Object.entries(e.details)) {
    if (campos.includes(campo)) setError(campo as Path<T>, { type: 'server', message: mensaje })
    else generales.push(mensaje)
  }
  if (Object.keys(e.details).length === 0) generales.push(e.message)
  return generales
}
