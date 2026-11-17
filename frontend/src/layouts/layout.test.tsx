import { screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { rutas } from '../router'
import ErrorPagina from '../pages/ErrorPagina'
import { iniciarComo } from '../test/sesion'
import { renderConProveedores } from '../test/utils'

describe('Navbar', () => {
  it('TEST-FE-GEN-003 anónimo: Ingresar y Registrarse, sin enlaces privados', async () => {
    renderConProveedores(rutas, '/')
    const nav = await screen.findByRole('navigation', { name: 'Principal' })
    expect(nav).toHaveTextContent('Eventos')
    expect(screen.getByRole('link', { name: 'Ingresar' })).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Registrarse' })).toBeInTheDocument()
    expect(screen.queryByRole('link', { name: 'Mis tickets' })).not.toBeInTheDocument()
  })

  it.each([
    ['USER', 'Mis tickets', ['Validar ingreso', 'Administrar eventos']],
    ['STAFF', 'Validar ingreso', ['Mis tickets', 'Administrar eventos']],
    ['ADMIN', 'Administrar eventos', ['Mis tickets']],
  ] as const)(
    'TEST-FE-GEN-003 %s ve "%s" y no los de otros roles',
    async (rol, visible, ocultos) => {
      iniciarComo([rol])
      renderConProveedores(rutas, '/')
      const nav = await screen.findByRole('navigation', { name: 'Principal' })
      expect(await screen.findByRole('button', { name: /andrea/ })).toBeInTheDocument()
      expect(nav).toHaveTextContent(visible)
      for (const o of ocultos) expect(nav).not.toHaveTextContent(o)
    },
  )

  it('TEST-FE-GEN-004 el menú móvil abre y cierra con el teclado', async () => {
    const user = userEvent.setup()
    renderConProveedores(rutas, '/')
    const boton = await screen.findByRole('button', { name: 'Abrir menú' })
    boton.focus()
    await user.keyboard('{Enter}')
    expect(screen.getByRole('navigation', { name: 'Principal (móvil)' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: 'Cerrar menú' })).toHaveAttribute(
      'aria-expanded',
      'true',
    )
    await user.keyboard('{Escape}')
    expect(screen.queryByRole('navigation', { name: 'Principal (móvil)' })).not.toBeInTheDocument()
  })
})

describe('ErrorPagina', () => {
  it('TEST-FE-GEN-002 muestra un mensaje seguro, sin stack ni detalles internos', async () => {
    const silenciar = vi.spyOn(console, 'error').mockImplementation(() => {})
    function Explota(): never {
      throw new Error('SECRETO-INTERNO en /var/app/Servicio.java:42')
    }
    renderConProveedores([{ path: '/', Component: Explota, ErrorBoundary: ErrorPagina }], '/')
    expect(await screen.findByText('¡Ups! Algo salió mal.')).toBeInTheDocument()
    expect(
      screen.getByText('Ocurrió un error inesperado. Inténtalo más tarde.'),
    ).toBeInTheDocument()
    expect(screen.queryByText(/SECRETO-INTERNO/)).not.toBeInTheDocument()
    silenciar.mockRestore()
  })
})
