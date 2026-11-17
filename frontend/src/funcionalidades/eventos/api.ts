import { z } from 'zod'
import { http } from '../../lib/http'
import { EventoResumenSchema, EventoSchema, type CrearEventoSolicitud } from './tipos'

const c = encodeURIComponent

export const listarEventosPublicados = () => http('/events/published', z.array(EventoResumenSchema))
export const listarEventosPorArtista = (stageName: string) =>
  http(`/events/by-artist?stageName=${c(stageName)}`, z.array(EventoResumenSchema))
export const obtenerEvento = (eventCode: string) => http(`/events/${c(eventCode)}`, EventoSchema)
export const crearEvento = (datos: CrearEventoSolicitud) =>
  http('/events', EventoSchema, { method: 'POST', body: datos })
export const publicarEvento = (eventCode: string) =>
  http(`/events/${c(eventCode)}/publish`, EventoSchema, { method: 'PATCH' })
export const asociarArtista = (eventCode: string, artistId: number) =>
  http(`/events/${c(eventCode)}/artists/${artistId}`, EventoSchema, { method: 'POST' })
