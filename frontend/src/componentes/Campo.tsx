import clsx from 'clsx'
import { useId, type ComponentProps } from 'react'

export const ESTILO_CONTROL =
  'block min-h-11 w-full rounded-lg border bg-white px-3 py-2 text-slate-900 focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600 dark:bg-slate-900 dark:text-slate-100'

interface CampoProps extends ComponentProps<'input'> {
  etiqueta: string
  error?: string | undefined
}

/** Con React Hook Form: <Campo etiqueta="…" error={errors.x?.message} {...register('x')} /> (en React 19 `ref` es una prop más). */
export default function Campo({ etiqueta, error, className, id, ...resto }: CampoProps) {
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
