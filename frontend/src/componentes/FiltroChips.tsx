import clsx from 'clsx'

interface FiltroChipsProps<V extends string> {
  opciones: { valor: V; etiqueta: string }[]
  valor: V
  onCambiar: (valor: V) => void
  etiquetaGrupo: string
}

/** DES-06: chips con aria-pressed (filtro de "Mis tickets"). */
export default function FiltroChips<V extends string>({
  opciones,
  valor,
  onCambiar,
  etiquetaGrupo,
}: FiltroChipsProps<V>) {
  return (
    <div role="group" aria-label={etiquetaGrupo} className="flex flex-wrap gap-2">
      {opciones.map((o) => (
        <button
          key={o.valor}
          type="button"
          aria-pressed={o.valor === valor}
          onClick={() => onCambiar(o.valor)}
          className={clsx(
            'min-h-11 rounded-full border px-4 text-sm font-medium focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-marca-600',
            o.valor === valor
              ? 'border-marca-600 bg-marca-600 text-white'
              : 'border-slate-300 bg-white text-slate-700 hover:bg-slate-50 dark:border-slate-600 dark:bg-slate-900 dark:text-slate-200',
          )}
        >
          {o.etiqueta}
        </button>
      ))}
    </div>
  )
}
