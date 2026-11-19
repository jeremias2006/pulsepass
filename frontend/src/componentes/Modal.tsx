/**
 * Ventana modal accesible. La usan la confirmación de compra y la de cancelación.
 *
 * <p>Un modal mal hecho es una trampa para quien navega con teclado o lector de pantalla: el
 * foco se queda en la página de atrás y se puede "escribir a ciegas" en lo que hay detrás. Este
 * implementa las cuatro reglas que lo evitan:</p>
 * <ol>
 *   <li>`role="dialog"` y `aria-modal="true"`: se anuncia como diálogo.</li>
 *   <li>`aria-labelledby`: el título del modal es su nombre accesible.</li>
 *   <li>Al abrirse, el foco entra en el modal; al cerrarse, vuelve al elemento que lo abrió.</li>
 *   <li>El foco queda atrapado dentro: el tabulador da la vuelta en lugar de salirse.</li>
 * </ol>
 */
import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'

interface ModalProps {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children: ReactNode
}

// Selector de los elementos que pueden recibir el foco, para saber cuál es el primero y cuál el
// último del modal. Se excluyen los deshabilitados y los que tienen tabindex="-1".
const FOCALIZABLES =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

/** Modal accesible: role="dialog", Esc cierra, el foco queda atrapado y se restaura al cerrar. */
export default function Modal({ abierto, titulo, onCerrar, children }: ModalProps) {
  const idTitulo = useId()
  const panel = useRef<HTMLDivElement>(null)

  // Se anota qué tenía el foco antes de abrir, se mueve el foco al primer elemento del modal, y
  // la función de limpieza lo devuelve a su sitio al cerrar. Sin eso, al cerrar el modal el foco
  // se perdería al principio de la página.
  useEffect(() => {
    if (!abierto) return
    const previo = document.activeElement as HTMLElement | null
    panel.current?.querySelector<HTMLElement>(FOCALIZABLES)?.focus()
    return () => previo?.focus()
  }, [abierto])

  if (!abierto) return null

  const alTeclear = (e: KeyboardEvent<HTMLDivElement>) => {
    if (e.key === 'Escape') {
      e.stopPropagation()
      onCerrar()
      return
    }
    // Trampa de foco: al llegar al último elemento, el tabulador vuelve al primero; con Shift,
    // al revés. Así el foco nunca se escapa a la página de detrás mientras el modal está abierto.
    if (e.key !== 'Tab' || !panel.current) return
    const items = [...panel.current.querySelectorAll<HTMLElement>(FOCALIZABLES)]
    const primero = items[0]
    const ultimo = items[items.length - 1]
    if (!primero || !ultimo) return
    if (e.shiftKey && document.activeElement === primero) {
      e.preventDefault()
      ultimo.focus()
    } else if (!e.shiftKey && document.activeElement === ultimo) {
      e.preventDefault()
      primero.focus()
    }
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-900/60 p-4">
      <div
        ref={panel}
        role="dialog"
        aria-modal="true"
        aria-labelledby={idTitulo}
        onKeyDown={alTeclear}
        className="w-full max-w-md rounded-xl bg-white p-6 shadow-xl dark:bg-slate-900"
      >
        <h2 id={idTitulo} className="mb-4 font-titulo text-lg font-semibold">
          {titulo}
        </h2>
        {children}
      </div>
    </div>
  )
}
