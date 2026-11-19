import { HttpResponse } from 'msw'

// Formas exactas de los DTO reales del backend.
export const SEDE = {
  id: 1,
  code: 'VEN-SMR-01',
  name: 'Marina Convention Center',
  city: 'Santa Marta',
  capacity: 3,
  active: true,
}

export const ARTISTAS = [
  { id: 1, stageName: 'Solar Beat', active: true },
  { id: 2, stageName: 'Neon Waves', active: true },
  { id: 3, stageName: 'Caribbean Sound', active: true },
]

export const RESUMEN = {
  id: 1,
  eventCode: 'CMF-2026',
  name: 'Caribbean Music Fest 2026',
  category: 'MUSIC',
  status: 'PUBLISHED',
  eventDate: '2027-03-20',
  venueName: 'Marina Convention Center',
}

export const EVENTO = {
  ...RESUMEN,
  description: 'Festival de música del Caribe.',
  minimumAge: 18,
  venueCode: 'VEN-SMR-01',
  artists: [ARTISTAS[0]],
}

export const TICKET = {
  id: 1,
  ticketCode: 'TKT-8F2A1C',
  type: 'VIP',
  price: 250000,
  status: 'PAID',
  purchaseDate: '2026-11-05T14:32:10',
  userEmail: 'andrea@pulsepass.test',
  eventCode: 'CMF-2026',
  eventName: 'Caribbean Music Fest 2026',
}

export const USUARIO = {
  id: 2,
  username: 'andrea',
  email: 'andrea@pulsepass.test',
  active: true,
  firstName: 'Andrea',
  lastName: 'Ruiz',
  phone: null,
  city: null,
  birthDate: '1998-04-12',
}

/** Respuesta de error con el contrato ErrorResponse del backend. */
export const errorApi = (status: number, message: string, details: Record<string, string> = {}) =>
  HttpResponse.json(
    { timestamp: '2026-11-16T10:00:00', status, error: 'Error', message, details },
    { status },
  )
