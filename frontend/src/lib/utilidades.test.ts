import { CrearEventoSchema } from '../funcionalidades/eventos/tipos'
import { RegistroSchema } from '../funcionalidades/usuarios/tipos'
import { traducirMensajeNegocio } from './errores'
import { formatearFecha, formatearPrecio } from './formato'
import { desdeSeguro } from './rutas'

describe('formato', () => {
  it('TEST-FE-LIB-005 PrecioCOP formatea 250000 como COP', () => {
    const s = formatearPrecio(250000)
    expect(s).toContain('250.000')
    expect(s).toContain('$')
  })
  it('formatearFecha no corre un día (LocalDate sin zona)', () => {
    expect(formatearFecha('2027-03-20')).toMatch(/20/)
    expect(formatearFecha('2027-03-01')).toMatch(/01/)
  })
})

describe('errores', () => {
  it('traduce los mensajes de negocio reales del backend', () => {
    expect(traducirMensajeNegocio('Only PAID tickets can be used. Current status: USED')).toBe(
      'El ticket ya fue utilizado.',
    )
    expect(traducirMensajeNegocio('User does not meet minimum age of 18 for event X')).toContain(
      'edad mínima',
    )
  })
  it('deja pasar un mensaje desconocido', () => {
    expect(traducirMensajeNegocio('algo nuevo')).toBe('algo nuevo')
  })
})

describe('desdeSeguro', () => {
  it('acepta rutas internas y rechaza lo demás', () => {
    expect(desdeSeguro({ desde: '/eventos/CMF-2026' })).toBe('/eventos/CMF-2026')
    expect(desdeSeguro({ desde: 'https://malo.com' })).toBe('/')
    expect(desdeSeguro({ desde: '//malo.com' })).toBe('/')
    expect(desdeSeguro(null)).toBe('/')
  })
})

describe('esquemas', () => {
  const base = {
    username: 'a',
    email: 'a@b.co',
    firstName: 'A',
    lastName: 'B',
    birthDate: '1998-04-12',
    password: 'x'.repeat(12),
  }
  it('registro válido', () => expect(RegistroSchema.safeParse(base).success).toBe(true))
  it('contraseña corta', () =>
    expect(RegistroSchema.safeParse({ ...base, password: 'corta' }).success).toBe(false))
  it('contraseña > 72 bytes aunque tenga < 72 caracteres', () => {
    const r = RegistroSchema.safeParse({ ...base, password: 'ñ'.repeat(40) }) // 40 caracteres = 80 bytes
    expect(r.success).toBe(false)
  })
  it('fecha de nacimiento futura', () =>
    expect(RegistroSchema.safeParse({ ...base, birthDate: '2999-01-01' }).success).toBe(false))
  it('evento: edad mínima NaN o negativa se rechaza', () => {
    const ev = {
      eventCode: 'C',
      name: 'N',
      description: '',
      category: 'MUSIC',
      eventDate: '2027-01-01',
      venueCode: 'V',
    }
    expect(CrearEventoSchema.safeParse({ ...ev, minimumAge: Number.NaN }).success).toBe(false)
    expect(CrearEventoSchema.safeParse({ ...ev, minimumAge: -1 }).success).toBe(false)
    expect(CrearEventoSchema.safeParse({ ...ev, minimumAge: 18 }).success).toBe(true)
  })
})
