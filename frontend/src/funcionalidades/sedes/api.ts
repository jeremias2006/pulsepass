import { z } from 'zod'
import { http } from '../../lib/http'
import { SedeSchema } from './tipos'

export const listarSedesActivas = () => http('/venues/active', z.array(SedeSchema))
export const obtenerSede = (code: string) => http(`/venues/${encodeURIComponent(code)}`, SedeSchema)
