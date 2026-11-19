import { http, HttpResponse } from 'msw'
import { ARTISTAS, EVENTO, RESUMEN, SEDE, TICKET, USUARIO } from './datos'

// Handlers por defecto (lectura). Cada prueba añade/sustituye los que necesite con server.use(...).
export const handlers = [
  http.post('*/api/auth/login', () =>
    HttpResponse.json({ accessToken: 'tok-123', tokenType: 'Bearer', expiresIn: 900 }),
  ),
  http.get('*/api/auth/me', () =>
    HttpResponse.json({
      id: 2,
      username: 'andrea',
      email: 'andrea@pulsepass.test',
      roles: ['USER'],
    }),
  ),
  http.get('*/api/users/by-email', () => HttpResponse.json(USUARIO)),
  http.get('*/api/venues/active', () => HttpResponse.json([SEDE])),
  http.get('*/api/venues/:code', () => HttpResponse.json(SEDE)),
  http.get('*/api/artists/active', () => HttpResponse.json(ARTISTAS)),
  http.get('*/api/artists/:id', ({ params }) =>
    HttpResponse.json(ARTISTAS.find((a) => String(a.id) === params.id) ?? ARTISTAS[0]),
  ),
  http.get('*/api/events/published', () => HttpResponse.json([RESUMEN])),
  http.get('*/api/events/by-artist', () => HttpResponse.json([RESUMEN])),
  http.get('*/api/events/:code', () => HttpResponse.json(EVENTO)),
  http.get('*/api/tickets/by-user', () => HttpResponse.json([TICKET])),
  http.get('*/api/tickets/:code', () => HttpResponse.json(TICKET)),
]
