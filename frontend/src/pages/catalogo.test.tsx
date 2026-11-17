import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { rutas } from '../router'
import { ARTISTAS, EVENTO, errorApi } from '../test/mocks/datos'
import { server } from '../test/server'
import { iniciarComo } from '../test/sesion'
import { renderConProveedores } from '../test/utils'

describe('cartelera', () => {
    it('TEST-FE-EVT-001 lista los eventos publicados (sin pedir login)', async () => {
        renderConProveedores(rutas, '/eventos')
        expect(await screen.findByText('Caribbean Music Fest 2026')).toBeInTheDocument()
        expect(screen.getByText('1 eventos')).toBeInTheDocument()
    })

    it('TEST-FE-EVT-002 lista vacía → estado vacío', async () => {
        server.use(http.get('*/api/events/published', () => HttpResponse.json([])))
        renderConProveedores(rutas, '/eventos')
        expect(await screen.findByText('Sin resultados para tu búsqueda')).toBeInTheDocument()
    })

    it('TEST-FE-EVT-003 el filtro por texto/categoría actualiza la URL', async () => {
        const user = userEvent.setup()
        const { router } = renderConProveedores(rutas, '/eventos')
        await screen.findByText('Caribbean Music Fest 2026')
        await user.type(screen.getByLabelText('Buscar por nombre'), 'zzz')
        expect(router.state.location.search).toContain('q=zzz')
        expect(await screen.findByText('Sin resultados para tu búsqueda')).toBeInTheDocument()
        await user.clear(screen.getByLabelText('Buscar por nombre'))
        await user.selectOptions(screen.getByLabelText('Categoría'), 'MUSIC')
        expect(router.state.location.search).toContain('categoria=MUSIC')
        expect(await screen.findByText('Caribbean Music Fest 2026')).toBeInTheDocument()
    })

    it('el filtro de la URL sobrevive a la recarga (se lee de ?q=)', async () => {
        renderConProveedores(rutas, '/eventos?q=zzz')
        expect(await screen.findByText('Sin resultados para tu búsqueda')).toBeInTheDocument()
        expect(screen.getByLabelText('Buscar por nombre')).toHaveValue('zzz')
    })
})

describe('detalle de evento', () => {
    it('TEST-FE-EVT-004 muestra evento, artistas y sede (ciudad y capacidad)', async () => {
        renderConProveedores(rutas, '/eventos/CMF-2026')
        expect(
            await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' }),
        ).toBeInTheDocument()
        expect(screen.getByText('Solar Beat')).toBeInTheDocument()
        expect(await screen.findByText(/Santa Marta · Capacidad 3/)).toBeInTheDocument()
    })

    it('TEST-FE-EVT-005 404 → "Evento no disponible"', async () => {
        server.use(http.get('*/api/events/:code', () => errorApi(404, 'Event not found: X')))
        renderConProveedores(rutas, '/eventos/X')
        expect(await screen.findByText('Evento no disponible')).toBeInTheDocument()
    })

    it('TEST-FE-EVT-006 evento SOLD_OUT → compra deshabilitada', async () => {
        iniciarComo(['USER'])
        server.use(
            http.get('*/api/events/:code', () => HttpResponse.json({ ...EVENTO, status: 'SOLD_OUT' })),
        )
        renderConProveedores(rutas, '/eventos/CMF-2026')
        expect(await screen.findByRole('button', { name: 'Comprar entrada' })).toBeDisabled()
        expect(screen.getByText(/no está disponible/)).toBeInTheDocument()
    })

    it('STAFF/ADMIN no ven el panel de compra', async () => {
        iniciarComo(['ADMIN'], 'admin@pulsepass.local', 'admin')
        renderConProveedores(rutas, '/eventos/CMF-2026')
        await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })
        expect(screen.queryByRole('button', { name: 'Comprar entrada' })).not.toBeInTheDocument()
    })

    it('ADMIN ve el código del evento y un enlace para gestionarlo; USER no', async () => {
        iniciarComo(['ADMIN'], 'admin@pulsepass.local', 'admin')
        const a = renderConProveedores(rutas, '/eventos/CMF-2026')
        const enlace = await screen.findByRole('link', { name: 'Gestionar evento →' })
        expect(enlace).toHaveAttribute('href', '/admin/eventos/CMF-2026')
        expect(screen.getByText('CMF-2026', { selector: 'strong' })).toBeInTheDocument()
        a.unmount()
        iniciarComo(['USER'])
        renderConProveedores(rutas, '/eventos/CMF-2026')
        await screen.findByRole('heading', { name: 'Caribbean Music Fest 2026' })
        expect(screen.queryByRole('link', { name: 'Gestionar evento →' })).not.toBeInTheDocument()
    })

    it('STAFF ve el código del evento con botón Copiar, pero no el enlace de gestión', async () => {
        iniciarComo(['STAFF'], 'laura@pulsepass.local', 'laura')
        renderConProveedores(rutas, '/eventos/CMF-2026')
        expect(await screen.findByText('CMF-2026', { selector: 'strong' })).toBeInTheDocument()
        expect(screen.getByRole('button', { name: 'Copiar código del evento' })).toBeInTheDocument()
        expect(screen.getByRole('link', { name: 'Ver tickets pagados →' })).toHaveAttribute(
            'href',
            '/staff/eventos/CMF-2026/tickets',
        )
        expect(screen.queryByRole('link', { name: 'Gestionar evento →' })).not.toBeInTheDocument()
    })
})

describe('artistas y sedes', () => {
    it('TEST-FE-ART-001 lista de artistas activos', async () => {
        renderConProveedores(rutas, '/artistas')
        expect(await screen.findByText('Neon Waves')).toBeInTheDocument()
    })

    it('TEST-FE-ART-002 detalle de artista con sus eventos', async () => {
        renderConProveedores(rutas, '/artistas/1')
        expect(await screen.findByRole('heading', { name: 'Solar Beat' })).toBeInTheDocument()
        expect(await screen.findByText('Caribbean Music Fest 2026')).toBeInTheDocument()
        expect(ARTISTAS).toHaveLength(3)
    })

    it('TEST-FE-ART-003 id no numérico → 404 sin llamar a la API', async () => {
        let llamadas = 0
        server.use(
            http.get('*/api/artists/:id', () => {
                llamadas++
                return HttpResponse.json(ARTISTAS[0])
            }),
        )
        renderConProveedores(rutas, '/artistas/abc')
        expect(await screen.findByText('Artista no encontrado')).toBeInTheDocument()
        expect(llamadas).toBe(0)
    })

    it('TEST-FE-VEN-001 lista y detalle de sede; 404 de sede', async () => {
        const a = renderConProveedores(rutas, '/sedes')
        expect(await screen.findByText('Marina Convention Center')).toBeInTheDocument()
        a.unmount()
        const b = renderConProveedores(rutas, '/sedes/VEN-SMR-01')
        expect(await screen.findByText('Capacidad: 3 personas')).toBeInTheDocument()
        b.unmount()
        server.use(http.get('*/api/venues/:code', () => errorApi(404, 'Venue not found')))
        renderConProveedores(rutas, '/sedes/NOPE')
        expect(await screen.findByText('Sede no encontrada')).toBeInTheDocument()
    })
})

describe('rutas', () => {
    it('TEST-FE-GEN-001 ruta inexistente → 404', async () => {
        renderConProveedores(rutas, '/no-existe')
        expect(await screen.findByText('No encontramos esta página')).toBeInTheDocument()
    })
})