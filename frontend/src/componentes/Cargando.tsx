/**
 * Indicador de carga en forma de esqueleto: bloques grises con pulso que ocupan el sitio del
 * contenido que está por llegar.
 *
 * <p>Se prefiere al típico círculo giratorio porque mantiene estable la altura de la página y
 * no produce el salto que se ve cuando el contenido real aparece de golpe.</p>
 *
 * <p>Lo muestran las pantallas mientras `isLoading` es cierto, y también los guardias de ruta
 * mientras la sesión está en estado 'cargando'.</p>
 */
export default function Cargando({ etiqueta = 'Cargando…' }: { etiqueta?: string }) {
  return (
    <div aria-busy="true" role="status" className="space-y-3">
      {/* Los bloques grises no significan nada para un lector de pantalla: este texto, oculto
          a la vista con sr-only pero presente en el HTML, es el que anuncia la espera. */}
      <span className="sr-only">{etiqueta}</span>
      <div className="h-6 w-1/3 animate-pulse rounded bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
      <div className="h-24 animate-pulse rounded-xl bg-slate-200 dark:bg-slate-800" />
    </div>
  )
}
