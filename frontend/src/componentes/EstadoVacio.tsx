/**
 * Bloque para "aquí no hay nada todavía": sin tickets, sin eventos publicados, sin resultados
 * de un filtro.
 *
 * <p>Es distinto de un error y conviene no confundirlos: una lista vacía es una respuesta
 * correcta del backend. Dejar la zona en blanco haría dudar al usuario de si falló algo.</p>
 *
 * <p>La prop `accion` permite ofrecer la salida natural: "Explorar eventos" cuando aún no se ha
 * comprado nada, por ejemplo.</p>
 */
import type { ReactNode } from 'react'

export default function EstadoVacio({ mensaje, accion }: { mensaje: string; accion?: ReactNode }) {
  return (
    <div className="rounded-xl border border-dashed border-slate-300 p-8 text-center dark:border-slate-700">
      <p className="text-slate-600 dark:text-slate-400">{mensaje}</p>
      {accion && <div className="mt-4">{accion}</div>}
    </div>
  )
}
