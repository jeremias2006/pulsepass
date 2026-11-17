import type { ReactNode } from 'react'

export default function EstadoVacio({ mensaje, accion }: { mensaje: string; accion?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center dark:border-slate-700">
      <p className="text-slate-600 dark:text-slate-400">{mensaje}</p>
      {accion && <div className="mt-4">{accion}</div>}
    </div>
  )
}
