/**
 * Mensaje de aviso en cuatro tonos: éxito, error, información y advertencia.
 *
 * <p>Se usa para los resultados de las acciones: "Ticket cancelado", "No cumples la edad
 * mínima", "Tu sesión expiró". El contenido es SIEMPRE texto, nunca HTML del servidor: así no
 * hay forma de inyectar marcado a través de un mensaje de error.</p>
 */
import clsx from 'clsx'
import { useEffect, useRef, type ReactNode } from 'react'

type Tono = 'exito' | 'error' | 'info' | 'advertencia'

const TONOS: Record<Tono, string> = {
  exito: 'border-emerald-300 bg-emerald-50 text-emerald-900',
  error: 'border-red-300 bg-red-50 text-red-900',
  info: 'border-sky-300 bg-sky-50 text-sky-900',
  advertencia: 'border-amber-300 bg-amber-50 text-amber-900',
}

interface AvisoProps {
  tono: Tono
  children: ReactNode
  onCerrar?: () => void
  /** Cierra solo tras N ms (PRD §9.20: éxito → 4000). Requiere `onCerrar`. */
  autoCerrarMs?: number
}

/** Siempre texto (nunca HTML). error/advertencia → role="alert"; éxito/info → role="status" (aria-live polite). */
export default function Aviso({ tono, children, onCerrar, autoCerrarMs }: AvisoProps) {
  // La referencia evita reiniciar el temporizador si el padre pasa una función nueva en cada render.
  const cerrarRef = useRef(onCerrar)
  useEffect(() => {
    cerrarRef.current = onCerrar
  })
  useEffect(() => {
    if (!autoCerrarMs) return
    const id = window.setTimeout(() => cerrarRef.current?.(), autoCerrarMs)
    return () => window.clearTimeout(id)
  }, [autoCerrarMs])

  // role="alert" interrumpe al lector de pantalla para anunciar el mensaje de inmediato; se
  // reserva a lo urgente. role="status" espera a que termine lo que esté leyendo. Usar "alert"
  // para todo acabaría cansando a quien depende de él.
  const urgente = tono === 'error' || tono === 'advertencia'
  return (
    <div
      role={urgente ? 'alert' : 'status'}
      className={clsx(
        'flex items-start justify-between gap-3 rounded-lg border px-4 py-3 text-sm',
        TONOS[tono],
      )}
    >
      <div>{children}</div>
      {onCerrar && (
        <button
          type="button"
          onClick={onCerrar}
          aria-label="Cerrar aviso"
          className="min-h-6 min-w-6 font-bold"
        >
          ×
        </button>
      )}
    </div>
  )
}
