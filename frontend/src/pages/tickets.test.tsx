import { screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { rutas } from '../router'
import { TICKET, errorApi } from '../test/mocks/datos'
import { server } from '../test/server'
import { iniciarComo } from '../test/sesion'
import { renderConProveedores } from '../test/utils'

const COMPRADO = { ...TICKET, ticketCode: 'TKT-NUEVO1', type: 'VIP' }

async function abrirModalDeCompra(user: ReturnType<typeof userEvent.setup>) {
  await user.click(await screen.findByLabelText('VIP'))
  await user.click(screen.getByRole('button', { name: 'Comprar entrada' }))
  return screen.findByRole('dialog', { name: 'Confirmar compra' })
}

describe('compra', () => {
  it('TEST-FE-TKT-001 compra válida → 201 y navega a la confirmación', async () => {
    iniciarComo(['USER'])
    server.use(
      http.post('*/api/tickets', () => HttpResponse.json(COMPRADO, { status: 201 })),
      http.get('*/api/tickets/TKT-NUEVO1', () => HttpResponse.json(COMPRADO)),
    )
    const user = userEvent.setup()
    const { router } = renderConProveedores(rutas, '/eventos/CMF-2026')
    const modal = await abrirModalDeCompra(user)
    await user.click(within(modal).getByRole('button', { name: 'Confirmar' }))
    await waitFor(() =>
      expect(router.state.location.pathname).toBe('/compra/confirmacion/TKT-NUEVO1'),
    )
    expect(await screen.findByText('¡Compra realizada con éxito!')).toBeInTheDocument()
  })

  it('TEST-FE-TKT-002 compra anónima → redirige a login y vuelve al evento', async () => {
    const user = userEvent.setup()
    const { router } = renderConProveedores(rutas, '/eventos/CMF-2026')
    await user.click(await screen.findByRole('link', { name: 'Inicia sesión para comprar' }))
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.state).toEqual({ desde: '/eventos/CMF-2026' })
  })

  it('TEST-FE-TKT-003 compra 409 → mensaje del backend (traducido) dentro del modal', async () => {
    iniciarComo(['USER'])
    server.use(
      http.post('*/api/tickets', () =>
        errorApi(409, 'User does not meet minimum age of 18 for event CMF-2026'),
      ),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/eventos/CMF-2026')
    const modal = await abrirModalDeCompra(user)
    await user.click(within(modal).getByRole('button', { name: 'Confirmar' }))
    expect(
      await within(modal).findByText('No cumples la edad mínima exigida para este evento.'),
    ).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Confirmar compra' })).toBeInTheDocument() // no se cierra
  })

  it('TEST-FE-TKT-004 la compra envía el email de la sesión (no un campo editable)', async () => {
    iniciarComo(['USER'], 'andrea@pulsepass.test')
    let cuerpo: unknown = null
    server.use(
      http.post('*/api/tickets', async ({ request }) => {
        cuerpo = await request.json()
        return HttpResponse.json(COMPRADO, { status: 201 })
      }),
      http.get('*/api/tickets/TKT-NUEVO1', () => HttpResponse.json(COMPRADO)),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/eventos/CMF-2026')
    const modal = await abrirModalDeCompra(user)
    await user.click(within(modal).getByRole('button', { name: 'Confirmar' }))
    await waitFor(() => expect(cuerpo).not.toBeNull())
    expect(cuerpo).toEqual({
      userEmail: 'andrea@pulsepass.test',
      eventCode: 'CMF-2026',
      type: 'VIP',
    })
  })
})

describe('mis tickets', () => {
  const USADO = { ...TICKET, id: 2, ticketCode: 'TKT-USADO1', status: 'USED' }

  it('TEST-FE-TKT-005 lista los tickets y filtra por estado', async () => {
    iniciarComo(['USER'])
    server.use(http.get('*/api/tickets/by-user', () => HttpResponse.json([TICKET, USADO])))
    const user = userEvent.setup()
    renderConProveedores(rutas, '/mis-tickets')
    expect(await screen.findByText('TKT-8F2A1C')).toBeInTheDocument()
    expect(screen.getByText('TKT-USADO1')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Usados' }))
    expect(screen.queryByText('TKT-8F2A1C')).not.toBeInTheDocument()
    expect(screen.getByText('TKT-USADO1')).toBeInTheDocument()
  })

  it('TEST-FE-TKT-006 sin tickets → estado vacío', async () => {
    iniciarComo(['USER'])
    server.use(http.get('*/api/tickets/by-user', () => HttpResponse.json([])))
    renderConProveedores(rutas, '/mis-tickets')
    expect(await screen.findByText('Aún no tienes tickets')).toBeInTheDocument()
  })

  it('TEST-FE-TKT-007 cancelar un ticket PAID → 200 y la lista se actualiza', async () => {
    iniciarComo(['USER'])
    let cancelado = false
    server.use(
      http.get('*/api/tickets/by-user', () =>
        HttpResponse.json([{ ...TICKET, status: cancelado ? 'CANCELLED' : 'PAID' }]),
      ),
      http.patch('*/api/tickets/TKT-8F2A1C/cancel', () => {
        cancelado = true
        return HttpResponse.json({ ...TICKET, status: 'CANCELLED' })
      }),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/mis-tickets')
    await user.click(await screen.findByRole('button', { name: 'Cancelar ticket' }))
    const modal = await screen.findByRole('dialog', { name: 'Cancelar ticket' })
    await user.click(within(modal).getByRole('button', { name: 'Sí, cancelar' }))
    expect(await screen.findByText('Cancelado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Cancelar ticket' })).not.toBeInTheDocument()
  })

  it('TEST-FE-TKT-008 cancelar → 409 muestra el mensaje y mantiene el modal', async () => {
    iniciarComo(['USER'])
    server.use(
      http.patch('*/api/tickets/TKT-8F2A1C/cancel', () =>
        errorApi(409, 'Ticket cannot be cancelled after the event date: TKT-8F2A1C'),
      ),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/mis-tickets')
    await user.click(await screen.findByRole('button', { name: 'Cancelar ticket' }))
    const modal = await screen.findByRole('dialog', { name: 'Cancelar ticket' })
    await user.click(within(modal).getByRole('button', { name: 'Sí, cancelar' }))
    expect(
      await within(modal).findByText(
        'No se puede cancelar un ticket después de la fecha del evento.',
      ),
    ).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: 'Cancelar ticket' })).toBeInTheDocument()
  })

  it('TEST-FE-TKT-010 el botón cancelar no se ofrece si status ≠ PAID', async () => {
    iniciarComo(['USER'])
    server.use(http.get('*/api/tickets/by-user', () => HttpResponse.json([USADO])))
    renderConProveedores(rutas, '/mis-tickets')
    await screen.findByText('TKT-USADO1')
    expect(screen.queryByRole('button', { name: 'Cancelar ticket' })).not.toBeInTheDocument()
  })
})

describe('detalle de ticket', () => {
  it('TEST-FE-TKT-009 ticket ajeno (403) → "Ticket no disponible"', async () => {
    iniciarComo(['USER'])
    server.use(http.get('*/api/tickets/:code', () => errorApi(403, 'forbidden')))
    renderConProveedores(rutas, '/mis-tickets/TKT-AJENO')
    expect(await screen.findByText('Ticket no disponible')).toBeInTheDocument()
  })

  it('muestra código, tipo, precio en COP y titular', async () => {
    iniciarComo(['USER'])
    renderConProveedores(rutas, '/mis-tickets/TKT-8F2A1C')
    expect(await screen.findByText('TKT-8F2A1C')).toBeInTheDocument()
    expect(screen.getByText(/250\.000/)).toBeInTheDocument()
    expect(screen.getByText('andrea@pulsepass.test')).toBeInTheDocument()
  })
})
