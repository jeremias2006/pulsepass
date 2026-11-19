import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { vi } from 'vitest'
import BotonCopiar from './BotonCopiar'

describe('BotonCopiar', () => {
  afterEach(() => vi.restoreAllMocks())

  it('copia el valor con la API del portapapeles y confirma «Copiado»', async () => {
    const u = userEvent.setup() // instala su propio navigator.clipboard
    render(<BotonCopiar valor="TKT-8F2A1C" etiqueta="código del ticket" />)
    await u.click(screen.getByRole('button', { name: 'Copiar código del ticket' }))
    expect(await screen.findByText('Copiado')).toBeInTheDocument()
    expect(await navigator.clipboard.readText()).toBe('TKT-8F2A1C')
  })

  it('sin navigator.clipboard usa el método alterno', async () => {
    const u = userEvent.setup()
    vi.spyOn(navigator, 'clipboard', 'get').mockReturnValue(undefined as never)
    const exec = vi.fn(() => true)
    Object.defineProperty(document, 'execCommand', { value: exec, configurable: true })
    render(<BotonCopiar valor="CMF-2026" etiqueta="código del evento" />)
    await u.click(screen.getByRole('button', { name: 'Copiar código del evento' }))
    await waitFor(() => expect(exec).toHaveBeenCalledWith('copy'))
    expect(await screen.findByText('Copiado')).toBeInTheDocument()
  })

  it('si no se puede copiar avisa', async () => {
    const u = userEvent.setup()
    vi.spyOn(navigator, 'clipboard', 'get').mockReturnValue(undefined as never)
    Object.defineProperty(document, 'execCommand', { value: () => false, configurable: true })
    render(<BotonCopiar valor="X" etiqueta="código del ticket" />)
    await u.click(screen.getByRole('button', { name: 'Copiar código del ticket' }))
    expect(await screen.findByText('No se pudo copiar')).toBeInTheDocument()
  })
})
