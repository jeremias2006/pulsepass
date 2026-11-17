import { useEffect, useRef, useState } from 'react'
import Boton from './Boton'

/** Copia al portapapeles; si la API no existe (p. ej. http en el celular por IP) usa execCommand. */
async function copiarTexto(valor: string): Promise<boolean> {
  try {
    if (navigator.clipboard) {
      await navigator.clipboard.writeText(valor)
      return true
    }
  } catch {
    /* se intenta el método alterno */
  }
  try {
    const area = document.createElement('textarea')
    area.value = valor
    area.setAttribute('readonly', '')
    area.style.position = 'fixed'
    area.style.opacity = '0'
    document.body.appendChild(area)
    area.select()
    const ok = document.execCommand('copy')
    area.remove()
    return ok
  } catch {
    return false
  }
}

interface Props {
  valor: string
  /** Qué se copia, para lectores de pantalla: «código del ticket», «código del evento». */
  etiqueta: string
}

export default function BotonCopiar({ valor, etiqueta }: Props) {
  const [estado, setEstado] = useState<'reposo' | 'copiado' | 'error'>('reposo')
  const temporizador = useRef<number | undefined>(undefined)
  useEffect(() => () => window.clearTimeout(temporizador.current), [])

  const copiar = async () => {
    const ok = await copiarTexto(valor)
    setEstado(ok ? 'copiado' : 'error')
    window.clearTimeout(temporizador.current)
    temporizador.current = window.setTimeout(() => setEstado('reposo'), 2000)
  }

  return (
    <Boton
      variante="fantasma"
      className="min-h-9 px-2 py-1"
      aria-label={`Copiar ${etiqueta}`}
      onClick={() => void copiar()}
    >
      <span aria-live="polite">
        {estado === 'copiado' ? 'Copiado' : estado === 'error' ? 'No se pudo copiar' : 'Copiar'}
      </span>
    </Boton>
  )
}
