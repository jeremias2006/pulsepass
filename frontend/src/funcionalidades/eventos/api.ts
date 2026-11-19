/**
 * Llamadas al backend para los eventos: una función por endpoint, sin lógica propia.
 *
 * <p>Cada función declara solo dos cosas: la RUTA y el ESQUEMA de la respuesta. Todo lo demás
 * (token, cabeceras, errores, validación) lo resuelve `lib/http`. Es el papel que cumple un
 * Repository en el backend: el acceso crudo al origen de datos, sin decidir nada.</p>
 *
 * <p>Dos listas distintas devuelven EventoResumen (menos campos, para las tarjetas) y el detalle
 * devuelve Evento (con descripción, edad mínima y artistas), igual que el backend distingue
 * EventSummaryResponse de EventResponse.</p>
 */
import { z } from 'zod'
import { http } from '../../lib/http'
import { EventoResumenSchema, EventoSchema, type CrearEventoSolicitud } from './tipos'

// Atajo: todo valor que venga del usuario o de la URL se escapa antes de incrustarlo en la ruta,
// para que un código con caracteres especiales no altere la dirección.
const c = encodeURIComponent

export const listarEventosPublicados = () => http('/events/published', z.array(EventoResumenSchema))
export const listarEventosPorArtista = (stageName: string) =>
  http(`/events/by-artist?stageName=${c(stageName)}`, z.array(EventoResumenSchema))
// Pública a nivel de URL, pero el backend oculta los borradores: un evento en DRAFT responde 404
// a quien no sea ADMIN, para no delatar su existencia.
export const obtenerEvento = (eventCode: string) => http(`/events/${c(eventCode)}`, EventoSchema)
// --- Operaciones de ADMIN (el backend exige hasRole("ADMIN") en SecurityConfig) ---
// El evento nace en DRAFT; no es visible al público hasta publicarlo.
export const crearEvento = (datos: CrearEventoSolicitud) =>
  http('/events', EventoSchema, { method: 'POST', body: datos })
// PATCH y no PUT: solo cambia el estado, no reemplaza el recurso. Al publicar, el backend
// revalida la fecha y que la sede siga activa, porque pudo pasar tiempo desde la creación.
export const publicarEvento = (eventCode: string) =>
  http(`/events/${c(eventCode)}/publish`, EventoSchema, { method: 'PATCH' })
export const asociarArtista = (eventCode: string, artistId: number) =>
  http(`/events/${c(eventCode)}/artists/${artistId}`, EventoSchema, { method: 'POST' })
