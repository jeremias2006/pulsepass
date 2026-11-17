import { http as msw, HttpResponse } from 'msw'
import { z } from 'zod'
import { errorApi } from '../test/mocks/datos'
import { server } from '../test/server'
import { ApiError, configurarHttp, http } from './http'

const alNoAutorizado = vi.fn()

beforeEach(() => {
  alNoAutorizado.mockReset()
  configurarHttp({ obtenerToken: () => 'tok-abc', alNoAutorizado })
})
afterAll(() => configurarHttp({ obtenerToken: () => null, alNoAutorizado: () => {} }))

describe('lib/http', () => {
  it('TEST-FE-LIB-001 añade Authorization Bearer cuando hay sesión', async () => {
    let auth: string | null = null
    server.use(
      msw.get('*/api/x', ({ request }) => {
        auth = request.headers.get('authorization')
        return HttpResponse.json({ ok: 1 })
      }),
    )
    await http('/x', z.object({ ok: z.number() }))
    expect(auth).toBe('Bearer tok-abc')
  })

  it('TEST-FE-LIB-002 ErrorResponse válido → ApiError con status y details', async () => {
    server.use(msw.post('*/api/x', () => errorApi(400, 'Validation failed', { email: 'inválido' })))
    const e = await http('/x', z.unknown(), { method: 'POST', body: {} }).catch((x: unknown) => x)
    expect(e).toBeInstanceOf(ApiError)
    expect(e).toMatchObject({
      status: 400,
      message: 'Validation failed',
      details: { email: 'inválido' },
    })
  })

  it('TEST-FE-LIB-003 cuerpo de error inválido → ApiError genérico', async () => {
    server.use(msw.get('*/api/x', () => new HttpResponse('<html>boom</html>', { status: 500 })))
    await expect(http('/x', z.unknown())).rejects.toMatchObject({
      status: 500,
      message: expect.stringContaining('inesperado'),
    })
  })

  it('TEST-FE-LIB-004 401 con token → dispara cierre de sesión; con ignorar401 no', async () => {
    server.use(msw.get('*/api/x', () => errorApi(401, 'no')))
    await expect(http('/x', z.unknown())).rejects.toBeInstanceOf(ApiError)
    expect(alNoAutorizado).toHaveBeenCalledTimes(1)
    await expect(http('/x', z.unknown(), { ignorar401: true })).rejects.toBeInstanceOf(ApiError)
    expect(alNoAutorizado).toHaveBeenCalledTimes(1)
  })

  it('error de red → ApiError con status 0', async () => {
    server.use(msw.get('*/api/x', () => HttpResponse.error()))
    await expect(http('/x', z.unknown())).rejects.toMatchObject({ status: 0 })
  })

  it('respuesta que no cumple el esquema zod → falla (no entra al estado de la app)', async () => {
    server.use(msw.get('*/api/x', () => HttpResponse.json({ ok: 'no es número' })))
    await expect(http('/x', z.object({ ok: z.number() }))).rejects.toThrow()
  })
})
