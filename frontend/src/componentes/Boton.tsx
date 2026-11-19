/**
 * Botón único de la aplicación, con cuatro aspectos.
 *
 * <p>Tener un solo componente en vez de estilizar cada <button> garantiza que todos los botones
 * del sitio se vean y se comporten igual, y que un cambio de color de marca se aplique en todas
 * partes a la vez.</p>
 *
 * <p>`extends ComponentProps<'button'>` hace que acepte además cualquier atributo de un botón
 * normal (onClick, disabled, aria-label, type...), que se reenvían con `{...resto}`.</p>
 */
import clsx from 'clsx'
import type { ComponentProps } from 'react'

type Variante = 'primario' | 'secundario' | 'peligro' | 'fantasma'

// Clases comunes a todas las variantes. Dos detalles de accesibilidad:
//   min-h-11      → 44 px de alto mínimo, el objetivo táctil recomendado para usar con el dedo.
//   focus-visible → contorno visible al navegar con el teclado, pero no al hacer clic con ratón.
const BASE =
  'inline-flex min-h-11 items-center justify-center gap-2 rounded-lg px-4 py-2 text-sm font-semibold transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 disabled:cursor-not-allowed disabled:opacity-60'

const VARIANTES: Record<Variante, string> = {
  primario: 'bg-marca-600 text-white hover:bg-marca-700',
  secundario:
    'border border-slate-300 bg-white text-slate-900 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-100 dark:hover:bg-slate-800',
  peligro: 'bg-red-600 text-white hover:bg-red-700',
  fantasma: 'text-marca-700 hover:bg-marca-50 dark:text-marca-500 dark:hover:bg-slate-800',
}

interface BotonProps extends ComponentProps<'button'> {
  variante?: Variante
  cargando?: boolean
}

export default function Boton({
  variante = 'primario',
  cargando = false,
  type = 'button', // los botones de envío deben pasar type="submit" explícitamente
  disabled,
  className,
  children,
  ...resto
}: BotonProps) {
  return (
    <button
      type={type}
      // Mientras carga queda deshabilitado: evita el doble clic que duplicaría una compra.
      disabled={disabled || cargando}
      // aria-busy avisa a los lectores de pantalla de que la acción está en curso.
      aria-busy={cargando || undefined}
      className={clsx(BASE, VARIANTES[variante], className)}
      {...resto}
    >
      {cargando ? 'Cargando…' : children}
    </button>
  )
}
