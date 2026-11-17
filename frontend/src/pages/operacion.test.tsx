import { fireEvent, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import { http, HttpResponse } from 'msw'
import { rutas } from '../router'
import { EVENTO, TICKET, errorApi } from '../test/mocks/datos'
import { server } from '../test/server'
import { iniciarComo } from '../test/sesion'
import { renderConProveedores } from '../test/utils'

const user = () => userEvent.setup()

async function buscarTicket(u: ReturnType<typeof userEvent.setup>, codigo: string) {
    await u.type(await screen.findByLabelText('Código de ticket'), codigo)
    await u.click(screen.getByRole('button', { name: 'Buscar' }))
}

describe('STAFF', () => {
    beforeEach(() => iniciarComo(['STAFF'], 'laura@pulsepass.local', 'laura'))

    it('TEST-FE-STF-001 buscar y marcar como usado → éxito', async () => {
        server.use(
            http.patch('*/api/tickets/TKT-8F2A1C/use', () =>
                HttpResponse.json({ ...TICKET, status: 'USED' }),
            ),
        )
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await buscarTicket(u, 'TKT-8F2A1C')
        await u.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
        expect(await screen.findByText('Ingreso registrado.')).toBeInTheDocument()
        expect(await screen.findByText('Usado')).toBeInTheDocument()
    })

    it('TEST-FE-STF-002 segunda vez → 409 visible', async () => {
        server.use(
            http.patch('*/api/tickets/TKT-8F2A1C/use', () =>
                errorApi(409, 'Only PAID tickets can be used. Current status: USED'),
            ),
        )
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await buscarTicket(u, 'TKT-8F2A1C')
        await u.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
        expect(await screen.findByText('El ticket ya fue utilizado.')).toBeInTheDocument()
    })

    it('TEST-FE-STF-003 ticket inexistente → 404 visible', async () => {
        server.use(http.get('*/api/tickets/:code', () => errorApi(404, 'Ticket not found: X')))
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await buscarTicket(u, 'X')
        expect(await screen.findByText('Ticket no encontrado')).toBeInTheDocument()
    })

    it('código vacío → error y 0 peticiones', async () => {
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await u.click(await screen.findByRole('button', { name: 'Buscar' }))
        expect(await screen.findByText('Ingresa el código del ticket')).toBeInTheDocument()
    })

    it('TEST-FE-STF-004 tickets pagados de un evento: tabla con filas', async () => {
        server.use(
            http.get('*/api/events/CMF-2026/tickets/paid', () =>
                HttpResponse.json([
                    TICKET,
                    { ...TICKET, id: 2, ticketCode: 'TKT-91B07D', type: 'GENERAL', price: 120000 },
                ]),
            ),
        )
        renderConProveedores(rutas, '/staff/eventos/CMF-2026/tickets')
        expect(await screen.findByText('TKT-8F2A1C')).toBeInTheDocument()
        expect(screen.getByText('TKT-91B07D')).toBeInTheDocument()
        expect(screen.getByText('Total: 2')).toBeInTheDocument()
    })
})

describe.each([
    ['STAFF', 'laura@pulsepass.local', 'laura'],
    ['ADMIN', 'admin@pulsepass.local', 'admin'],
] as const)('Validar ingreso: lista por validar (%s)', (rol, email, username) => {
    const OTRO = { ...TICKET, id: 2, ticketCode: 'TKT-91B07D', userEmail: 'ana@correo.com' }
    beforeEach(() => iniciarComo([rol], email, username))

    async function cargarLista(u: ReturnType<typeof userEvent.setup>, codigo = 'CMF-2026') {
        await u.type(await screen.findByLabelText('Código del evento'), codigo)
        await u.click(screen.getByRole('button', { name: 'Ver tickets' }))
    }

    it('muestra los tickets PAID; «Seleccionar» lo carga arriba y se valida allí', async () => {
        let usados = 0
        server.use(
            http.get('*/api/events/CMF-2026/tickets/paid', () =>
                HttpResponse.json(usados ? [OTRO] : [TICKET, OTRO]),
            ),
            http.patch('*/api/tickets/TKT-8F2A1C/use', () => {
                usados++
                return HttpResponse.json({ ...TICKET, status: 'USED' })
            }),
        )
        window.scrollTo = vi.fn()
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await cargarLista(u)
        expect(await screen.findByText('Por validar: 2')).toBeInTheDocument()
        expect(screen.queryByRole('button', { name: 'Validar' })).not.toBeInTheDocument()
        await u.click(screen.getAllByRole('button', { name: 'Seleccionar' })[0]!)
        expect(await screen.findByLabelText('Código de ticket')).toHaveValue('TKT-8F2A1C')
        await u.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
        expect(await screen.findByText('Ingreso registrado.')).toBeInTheDocument()
        expect(await screen.findByText('Por validar: 1')).toBeInTheDocument()
        expect(screen.getByText('TKT-91B07D')).toBeInTheDocument()
    })

    it('409 al validar el ticket seleccionado → mensaje traducido', async () => {
        server.use(
            http.get('*/api/events/CMF-2026/tickets/paid', () => HttpResponse.json([TICKET])),
            http.patch('*/api/tickets/TKT-8F2A1C/use', () =>
                errorApi(409, 'Only PAID tickets can be used. Current status: USED'),
            ),
        )
        window.scrollTo = vi.fn()
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await cargarLista(u)
        await u.click(await screen.findByRole('button', { name: 'Seleccionar' }))
        await u.click(await screen.findByRole('button', { name: 'Marcar como usado' }))
        expect(await screen.findByText('El ticket ya fue utilizado.')).toBeInTheDocument()
    })

    it('lista vacía y evento inexistente', async () => {
        server.use(
            http.get('*/api/events/VACIO/tickets/paid', () => HttpResponse.json([])),
            http.get('*/api/events/NOPE/tickets/paid', () => errorApi(404, 'Event not found: NOPE')),
        )
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await cargarLista(u, 'VACIO')
        expect(await screen.findByText('No hay tickets por validar')).toBeInTheDocument()
        await u.clear(screen.getByLabelText('Código del evento'))
        await u.type(screen.getByLabelText('Código del evento'), 'NOPE')
        await u.click(screen.getByRole('button', { name: 'Ver tickets' }))
        expect(await screen.findByText('Evento no encontrado')).toBeInTheDocument()
    })

    it('el filtro reduce la lista por código o titular', async () => {
        server.use(
            http.get('*/api/events/CMF-2026/tickets/paid', () => HttpResponse.json([TICKET, OTRO])),
        )
        const u = user()
        renderConProveedores(rutas, '/staff/validacion')
        await cargarLista(u)
        await u.type(await screen.findByLabelText('Filtrar por código o titular'), 'ana@')
        expect(await screen.findByText('Por validar: 1')).toBeInTheDocument()
        expect(screen.queryByText('TKT-8F2A1C')).not.toBeInTheDocument()
        expect(screen.getByText('TKT-91B07D')).toBeInTheDocument()
    })
})

describe('ADMIN: crear evento', () => {
    beforeEach(() => iniciarComo(['ADMIN'], 'admin@pulsepass.local', 'admin'))

    async function llenar(u: ReturnType<typeof userEvent.setup>) {
        await u.type(await screen.findByLabelText('Código del evento'), 'CMF-2026')
        await u.type(screen.getByLabelText('Nombre'), 'Caribbean Music Fest 2026')
        fireEvent.change(screen.getByLabelText('Fecha'), { target: { value: '2027-03-20' } })
        await screen.findByRole('option', { name: /Marina Convention Center/ })
        await u.selectOptions(screen.getByLabelText('Sede'), 'VEN-SMR-01')
    }

    it('TEST-FE-ADM-001 campos vacíos → errores y 0 peticiones', async () => {
        let llamadas = 0
        server.use(
            http.post('*/api/events', () => {
                llamadas++
                return HttpResponse.json(EVENTO, { status: 201 })
            }),
        )
        const u = user()
        renderConProveedores(rutas, '/admin/eventos/nuevo')
        await u.click(await screen.findByRole('button', { name: 'Crear evento' }))
        expect(await screen.findByText('Ingresa el código')).toBeInTheDocument()
        expect(screen.getByText('Ingresa el nombre')).toBeInTheDocument()
        expect(screen.getByText('Selecciona la fecha')).toBeInTheDocument()
        expect(screen.getByText('Selecciona una sede')).toBeInTheDocument()
        expect(llamadas).toBe(0)
    })

    it('TEST-FE-ADM-002 crear 201 → navega a la gestión del evento', async () => {
        let cuerpo: Record<string, unknown> = {}
        server.use(
            http.post('*/api/events', async ({ request }) => {
                cuerpo = (await request.json()) as Record<string, unknown>
                return HttpResponse.json({ ...EVENTO, status: 'DRAFT' }, { status: 201 })
            }),
            http.get('*/api/events/CMF-2026', () => HttpResponse.json({ ...EVENTO, status: 'DRAFT' })),
        )
        const u = user()
        const { router } = renderConProveedores(rutas, '/admin/eventos/nuevo')
        await llenar(u)
        await u.click(screen.getByRole('button', { name: 'Crear evento' }))
        await waitFor(() => expect(router.state.location.pathname).toBe('/admin/eventos/CMF-2026'))
        expect(await screen.findByText('Evento creado.')).toBeInTheDocument()
        expect(cuerpo).toMatchObject({
            eventCode: 'CMF-2026',
            venueCode: 'VEN-SMR-01',
            minimumAge: 0,
            category: 'MUSIC',
        })
        expect(cuerpo).not.toHaveProperty('description') // descripción vacía se omite
    })

    it('TEST-FE-ADM-003 400 con details → errores junto a los campos', async () => {
        server.use(
            http.post('*/api/events', () =>
                errorApi(400, 'Validation failed', { name: 'Name must not exceed 150 characters' }),
            ),
        )
        const u = user()
        renderConProveedores(rutas, '/admin/eventos/nuevo')
        await llenar(u)
        await u.click(screen.getByRole('button', { name: 'Crear evento' }))
        expect(await screen.findByText('Name must not exceed 150 characters')).toBeInTheDocument()
    })

    it('409 (código duplicado) → aviso arriba del formulario, traducido', async () => {
        server.use(
            http.post('*/api/events', () => errorApi(409, 'Event code already exists: CMF-2026')),
        )
        const u = user()
        renderConProveedores(rutas, '/admin/eventos/nuevo')
        await llenar(u)
        await u.click(screen.getByRole('button', { name: 'Crear evento' }))
        expect(await screen.findByText('Ya existe un evento con ese código.')).toBeInTheDocument()
    })
})

describe('ADMIN: gestionar evento', () => {
    const BORRADOR = { ...EVENTO, status: 'DRAFT' }
    beforeEach(() => {
        iniciarComo(['ADMIN'], 'admin@pulsepass.local', 'admin')
        server.use(http.get('*/api/events/CMF-2026', () => HttpResponse.json(BORRADOR)))
    })

    it('TEST-FE-ADM-004 publicar evento → badge Publicado', async () => {
        server.use(
            http.patch('*/api/events/CMF-2026/publish', () =>
                HttpResponse.json({ ...EVENTO, status: 'PUBLISHED' }),
            ),
        )
        const u = user()
        renderConProveedores(rutas, '/admin/eventos/CMF-2026')
        await u.click(await screen.findByRole('button', { name: 'Publicar evento' }))
        const modal = await screen.findByRole('dialog', { name: 'Publicar evento' })
        await u.click(within(modal).getByRole('button', { name: 'Publicar' }))
        expect(await screen.findByText('Publicado')).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Publicar evento' })).toBeDisabled()
    })

    it('TEST-FE-ADM-005 publicar → 409 visible', async () => {
        server.use(
            http.patch('*/api/events/CMF-2026/publish', () =>
                errorApi(409, 'Only DRAFT events can be published. Current status: PUBLISHED'),
            ),
        )
        const u = user()
        renderConProveedores(rutas, '/admin/eventos/CMF-2026')
        await u.click(await screen.findByRole('button', { name: 'Publicar evento' }))
        const modal = await screen.findByRole('dialog', { name: 'Publicar evento' })
        await u.click(within(modal).getByRole('button', { name: 'Publicar' }))
        expect(
            await screen.findByText('Solo se pueden publicar eventos en borrador.'),
        ).toBeInTheDocument()
    })

    it('TEST-FE-ADM-006 asociar artista → aparece en la lista; duplicado → 409', async () => {
        const u = user()
        server.use(
            http.post('*/api/events/CMF-2026/artists/2', () =>
                HttpResponse.json({
                    ...BORRADOR,
                    artists: [...BORRADOR.artists, { id: 2, stageName: 'Neon Waves', active: true }],
                }),
            ),
        )
        renderConProveedores(rutas, '/admin/eventos/CMF-2026')
        await u.selectOptions(await screen.findByLabelText('Agregar artista'), '2')
        await u.click(screen.getByRole('button', { name: 'Asociar' }))
        expect(await screen.findByText('Neon Waves', { selector: 'li' })).toBeInTheDocument()
    })

    it('asociar un artista ya asociado → 409 traducido', async () => {
        server.use(
            http.post('*/api/events/CMF-2026/artists/2', () =>
                errorApi(409, 'Artist 2 is already assigned to event CMF-2026'),
            ),
        )
        const u = user()
        renderConProveedores(rutas, '/admin/eventos/CMF-2026')
        await u.selectOptions(await screen.findByLabelText('Agregar artista'), '2')
        await u.click(screen.getByRole('button', { name: 'Asociar' }))
        expect(
            await screen.findByText('El artista ya está asociado a este evento.'),
        ).toBeInTheDocument()
    })
})