import { fireEvent, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { http, HttpResponse } from 'msw'
import { CLAVE_SESION } from '../funcionalidades/auth/tipos'
import { rutas } from '../router'
import { USUARIO, errorApi } from '../test/mocks/datos'
import { server } from '../test/server'
import { iniciarComo } from '../test/sesion'
import { renderConProveedores } from '../test/utils'

async function llenarLogin(user: ReturnType<typeof userEvent.setup>) {
  await user.type(await screen.findByLabelText('Usuario o correo'), 'andrea')
  await user.type(screen.getByLabelText('Contraseña'), 'una-clave-larga-123')
  await user.click(screen.getByRole('button', { name: 'Ingresar' }))
}

async function llenarRegistro(user: ReturnType<typeof userEvent.setup>, password: string) {
  await user.type(await screen.findByLabelText('Nombre de usuario'), 'andrea')
  await user.type(screen.getByLabelText('Correo electrónico'), 'andrea@pulsepass.test')
  await user.type(screen.getByLabelText('Nombres'), 'Andrea')
  await user.type(screen.getByLabelText('Apellidos'), 'Ruiz')
  fireEvent.change(screen.getByLabelText('Fecha de nacimiento'), {
    target: { value: '1998-04-12' },
  })
  await user.type(screen.getByLabelText('Contraseña (mín. 12)'), password)
  await user.click(screen.getByRole('button', { name: 'Crear cuenta' }))
}

describe('login', () => {
  it('TEST-FE-AUTH-001 login válido → sesión autenticada y vuelta a la ruta de origen', async () => {
    const user = userEvent.setup()
    const { router } = renderConProveedores(rutas, '/cuenta')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    await llenarLogin(user)
    expect(await screen.findByRole('heading', { name: 'Mi cuenta' })).toBeInTheDocument()
    expect(router.state.location.pathname).toBe('/cuenta')
    expect(sessionStorage.getItem(CLAVE_SESION)).toContain('tok-123')
  })

  it('TEST-FE-AUTH-002 login 401 → "Credenciales inválidas" (sin marcar sesión expirada)', async () => {
    server.use(http.post('*/api/auth/login', () => errorApi(401, 'Bad credentials')))
    const user = userEvent.setup()
    renderConProveedores(rutas, '/login')
    await llenarLogin(user)
    expect(await screen.findByText('Credenciales inválidas')).toBeInTheDocument()
    expect(sessionStorage.getItem('pulsepass.aviso')).toBeNull()
  })

  it('TEST-FE-AUTH-003 login sin campos → errores y 0 peticiones', async () => {
    let llamadas = 0
    server.use(
      http.post('*/api/auth/login', () => {
        llamadas++
        return HttpResponse.json({})
      }),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/login')
    await user.click(await screen.findByRole('button', { name: 'Ingresar' }))
    expect(await screen.findByText('Ingresa tu usuario o correo')).toBeInTheDocument()
    expect(screen.getByText('Ingresa tu contraseña')).toBeInTheDocument()
    expect(llamadas).toBe(0)
  })
})

describe('registro', () => {
  it('TEST-FE-AUTH-004 contraseña corta → error y 0 peticiones', async () => {
    let llamadas = 0
    server.use(
      http.post('*/api/users', () => {
        llamadas++
        return HttpResponse.json({})
      }),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/registro')
    await llenarRegistro(user, 'corta')
    expect(await screen.findByText('Mínimo 12 caracteres')).toBeInTheDocument()
    expect(llamadas).toBe(0)
  })

  it('TEST-FE-AUTH-005 registro 201 → redirige a /login con aviso', async () => {
    server.use(http.post('*/api/users', () => HttpResponse.json(USUARIO, { status: 201 })))
    const user = userEvent.setup()
    const { router } = renderConProveedores(rutas, '/registro')
    await llenarRegistro(user, 'una-clave-larga-123')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(await screen.findByText('Cuenta creada. Ahora inicia sesión.')).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-006 registro 409 → mensaje genérico, sin datos internos', async () => {
    server.use(
      http.post('*/api/users', () => errorApi(409, 'Email already exists: andrea@pulsepass.test')),
    )
    const user = userEvent.setup()
    renderConProveedores(rutas, '/registro')
    await llenarRegistro(user, 'una-clave-larga-123')
    expect(await screen.findByText('El usuario o correo ya está registrado')).toBeInTheDocument()
    expect(screen.queryByText(/Email already exists/)).not.toBeInTheDocument()
  })
})

describe('guardas y sesión', () => {
  it('TEST-FE-AUTH-007 RutaProtegida: anónimo → /login con state.desde', async () => {
    const { router } = renderConProveedores(rutas, '/cuenta')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(router.state.location.state).toEqual({ desde: '/cuenta' })
  })

  it('TEST-FE-AUTH-008 RutaPorRol: USER en /admin/eventos → /403', async () => {
    iniciarComo(['USER'])
    const { router } = renderConProveedores(rutas, '/admin/eventos')
    await waitFor(() => expect(router.state.location.pathname).toBe('/403'))
    expect(await screen.findByText(/No tienes permisos/)).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-009 RutaPorRol: ADMIN en /admin/eventos → renderiza', async () => {
    iniciarComo(['ADMIN'], 'admin@pulsepass.local', 'admin')
    renderConProveedores(rutas, '/admin/eventos')
    expect(
      await screen.findByRole('heading', { name: 'Administración de eventos' }),
    ).toBeInTheDocument()
  })

  it('TEST-FE-AUTH-010 cerrar sesión limpia token y caché', async () => {
    iniciarComo(['USER'])
    const user = userEvent.setup()
    const { queryClient } = renderConProveedores(rutas, '/cuenta')
    await screen.findByRole('heading', { name: 'Mi cuenta' })
    expect(queryClient.getQueryCache().getAll().length).toBeGreaterThan(0)
    await user.click(screen.getByRole('button', { name: /andrea/ }))
    await user.click(screen.getByRole('menuitem', { name: 'Salir' }))
    await waitFor(() => expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull())
    // Tras salir no queda ningún dato del usuario en caché (la página ya redirigió a /login).
    await screen.findByRole('heading', { name: 'Inicia sesión' })
    expect(
      JSON.stringify(
        queryClient
          .getQueryCache()
          .getAll()
          .map((q) => q.state.data),
      ),
    ).not.toContain('andrea@pulsepass.test')
  })

  it('TEST-FE-AUTH-011 token expirado al restaurar → anónimo', async () => {
    sessionStorage.setItem(CLAVE_SESION, JSON.stringify({ token: 't', expiraEn: Date.now() - 1 }))
    const { router } = renderConProveedores(rutas, '/cuenta')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(sessionStorage.getItem(CLAVE_SESION)).toBeNull()
  })

  it('un 401 con sesión activa cierra la sesión y vuelve a /login con aviso', async () => {
    iniciarComo(['USER'])
    server.use(http.get('*/api/tickets/by-user', () => errorApi(401, 'Token vencido')))
    const { router } = renderConProveedores(rutas, '/mis-tickets')
    await waitFor(() => expect(router.state.location.pathname).toBe('/login'))
    expect(await screen.findByText('Tu sesión expiró. Inicia sesión de nuevo.')).toBeInTheDocument()
  })
})
