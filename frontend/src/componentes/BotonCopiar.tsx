/**
 * Botón para copiar un código al portapapeles, con confirmación visual.
 *
 * <p>Pensado para los códigos de ticket y de evento, que son largos y fáciles de transcribir
 * mal. Lo usan la tarjeta de ticket, la confirmación de compra y la pantalla de validación.</p>
 */
import { useEffect, useRef, useState } from 'react'
import Boton from './Boton'

/**
 * Copia al portapapeles; si la API no existe (p. ej. http en el celular por IP) usa execCommand.
 *
 * <p>`navigator.clipboard` solo está disponible en contextos seguros (HTTPS o localhost). Al
 * probar desde un móvil contra la IP del equipo, la conexión es HTTP plano y no existe, así que
 * se recurre al método antiguo: crear un <textarea> invisible, seleccionarlo y copiar. Devuelve
 * un booleano en vez de lanzar, para que quien llame decida qué mostrar.</p>
 */
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
  // Limpieza al desmontar: si el usuario cambia de pantalla justo después de copiar, el
  // temporizador pendiente intentaría actualizar un componente que ya no existe.
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
      {/* aria-live="polite" hace que el cambio de texto a "Copiado" se anuncie al lector de
          pantalla: sin él, quien no ve el botón no sabría si la acción funcionó. */}
      <span aria-live="polite">
        {estado === 'copiado' ? 'Copiado' : estado === 'error' ? 'No se pudo copiar' : 'Copiar'}
      </span>
    </Boton>
  )
}
