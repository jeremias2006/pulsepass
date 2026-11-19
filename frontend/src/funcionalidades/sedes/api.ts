/**
 * Llamadas al backend para las sedes. Ambas son públicas (permitAll en SecurityConfig): el
 * catálogo de sedes es información de consulta, no hay escrituras desde el frontend.
 */
import { z } from 'zod'
import { http } from '../../lib/http'
import { SedeSchema } from './tipos'

export const listarSedesActivas = () => http('/venues/active', z.array(SedeSchema))
export const obtenerSede = (code: string) => http(`/venues/${encodeURIComponent(code)}`, SedeSchema)
