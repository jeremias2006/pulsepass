/**
 * Campo de texto con su etiqueta y su mensaje de error, listo para React Hook Form.
 *
 * <p>Agrupa las tres piezas que siempre van juntas (label, input y error) y resuelve el enlace
 * accesible entre ellas, que es la parte que suele hacerse mal:</p>
 * <ul>
 *   <li>`htmlFor` ↔ `id`: al pulsar la etiqueta se enfoca el campo, y el lector de pantalla
 *       anuncia de qué campo se trata.</li>
 *   <li>`aria-invalid`: marca el campo como erróneo.</li>
 *   <li>`aria-describedby`: enlaza el campo con el texto del error, para que se lea junto a él.</li>
 *   <li>`role="alert"` en el error: lo anuncia en cuanto aparece.</li>
 * </ul>
 */
import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'

// Estilo compartido con Selector, para que un <input> y un <select> se vean idénticos.
export const ESTILO_CONTROL =
  'block min-h-11 w-full rounded-lg border bg-white px-3 py-2 text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:bg-slate-900 dark:text-slate-100'

interface CampoProps extends ComponentProps<'input'> {
  etiqueta: string
  error?: string | undefined
}

/** Con React Hook Form: <Campo etiqueta="…" error={errors.x?.message} {...register('x')} /> (en React 19 `ref` es una prop más). */
export default function Campo({ etiqueta, error, className, id, ...resto }: CampoProps) {
  // useId genera un identificador único y estable por instancia. Hace falta porque el mismo
  // componente puede aparecer varias veces en una página y los id del HTML no pueden repetirse.
  const auto = useId()
  const idCampo = id ?? auto
  const idError = `${idCampo}-error`
  return (
    <div>
      <label htmlFor={idCampo} className="mb-1 block text-sm font-medium">
        {etiqueta}
      </label>
      <input
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : undefined}
        className={clsx(
          ESTILO_CONTROL,
          error ? 'border-red-600' : 'border-slate-300 dark:border-slate-600',
          className,
        )}
        {...resto}
      />
      {error && (
        <p id={idError} role="alert" className="mt-1 text-sm text-red-700 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  )
}
