/**
 * Contenedor con fondo, bordes redondeados y sombra: el recuadro base de toda la interfaz.
 *
 * <p>Existe para no repetir las mismas clases en cada bloque y para que todos los recuadros del
 * sitio compartan radio, sombra y color de fondo, también en modo oscuro. `className` permite
 * añadir espaciados concretos sin perder esa base.</p>
 */
import clsx from 'clsx'
import type { ReactNode } from 'react'

export default function Tarjeta({
  children,
  className,
}: {
  children: ReactNode
  className?: string
}) {
  return (
    <div className={clsx('rounded-xl bg-white p-4 shadow-sm dark:bg-slate-900', className)}>
      {children}
    </div>
  )
}
