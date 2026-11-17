import { useEffect, useId, useRef, type KeyboardEvent, type ReactNode } from 'react'

interface ModalProps {
  abierto: boolean
  titulo: string
  onCerrar: () => void
  children: ReactNode
}

const FOCALIZABLES =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'

/** Modal accesible: role="dialog", Esc cierra, el foco queda atrapado y se restaura al cerrar. */
export default function Modal({ abierto, titulo, onCerrar, children }: ModalProps) {
  const idTitulo = useId()
  const panel = useRef<HTMLDivElement>(null)

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
