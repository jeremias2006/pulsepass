/**
 * Lista desplegable con etiqueta y error. Es el gemelo de `Campo` para los <select>, con el
 * mismo tratamiento de accesibilidad (useId, aria-invalid, aria-describedby, role="alert").
 *
 * <p>Se usa, por ejemplo, para elegir la sede y la categoría al crear un evento.</p>
 */
import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'
// Reutiliza el estilo de Campo para que ambos controles se vean iguales en el formulario.
import { ESTILO_CONTROL } from './Campo'

interface SelectorProps extends ComponentProps<'select'> {
  etiqueta: string
  error?: string | undefined
}

export default function Selector({
  etiqueta,
  error,
  className,
  id,
  children,
  ...resto
}: SelectorProps) {
  const auto = useId()
  const idCampo = id ?? auto
  const idError = `${idCampo}-error`
  return (
    <div>
      <label htmlFor={idCampo} className="mb-1 block text-sm font-medium">
        {etiqueta}
      </label>
      <select
        id={idCampo}
        aria-invalid={error ? true : undefined}
        aria-describedby={error ? idError : undefined}
        className={clsx(
          ESTILO_CONTROL,
          error ? 'border-red-600' : 'border-slate-300 dark:border-slate-600',
          className,
        )}
        {...resto}
      >
        {children}
      </select>
      {error && (
        <p id={idError} role="alert" className="mt-1 text-sm text-red-700 dark:text-red-400">
          {error}
        </p>
      )}
    </div>
  )
}
