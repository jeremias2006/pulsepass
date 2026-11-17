export default function Cargando({ etiqueta = 'Cargando…' }: { etiqueta?: string }) {
  return (
    <div aria-busy="true" role="status" className="space-y-3">
      <span className="sr-only">{etiqueta}</span>
      <div className="h-6 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
    </div>
  )
}
