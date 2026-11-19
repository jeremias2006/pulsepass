import { useSearchParams } from 'react-router'
import Boton from '../componentes/Boton'
import Campo from '../componentes/Campo'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import Selector from '../componentes/Selector'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { CATEGORIAS } from '../funcionalidades/eventos/categorias'
import TarjetaEvento from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPorArtista, useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { EventCategorySchema } from '../funcionalidades/eventos/tipos'

/**
 * Catálogo de eventos (ruta `/eventos`). Caso de uso: explorar y filtrar la oferta.
 *
 * <p><b>Acceso:</b> pública.<br>
 * <b>Consume:</b> GET /events/published, o GET /events/by-artist si hay filtro de artista, más
 * GET /artists/active para poblar el desplegable.</p>
 *
 * <p>El filtro por artista se resuelve en el backend (es otro endpoint), mientras que el texto y
 * la categoría se filtran aquí sobre la lista ya descargada: el backend no ofrece búsqueda y el
 * catálogo es pequeño.</p>
 */
/** Filtros en la URL (?q=&categoria=&artista=): sobreviven a la recarga y se pueden compartir (FR-FE-EVT-002). */
export default function CatalogoEventos() {
  // Guardar los filtros en la URL y no en el estado del componente tiene tres ventajas: se
  // conservan al recargar, el enlace se puede compartir tal cual, y el botón "atrás" deshace
  // el último filtro en vez de salir de la página.
  const [params, setParams] = useSearchParams()
  const q = params.get('q') ?? ''
  // La URL es un dato externo que cualquiera puede escribir: ?categoria=CUALQUIERCOSA. safeParse
  // devuelve undefined si no es una categoría válida, y entonces simplemente no se filtra.
  const categoria = EventCategorySchema.safeParse(params.get('categoria')).data // valida el dato de la URL
  const artista = params.get('artista')

  // Se declaran los dos hooks siempre (no se pueden llamar condicionalmente) y se elige cuál
  // mirar. El que no corresponde queda inactivo por su `enabled`, así que no llama al backend.
  const publicados = useEventosPublicados()
  const porArtista = useEventosPorArtista(artista)
  const artistas = useArtistasActivos()
  const consulta = artista ? porArtista : publicados

  const cambiar = (clave: string, valor: string) =>
    setParams(
      (prev) => {
        const sig = new URLSearchParams(prev)
        if (valor) sig.set(clave, valor)
        else sig.delete(clave)
        return sig
      },
      { replace: true },
    )

  // Texto y categoría filtran en el cliente (la API no tiene búsqueda en servidor).
  const visibles = (consulta.data ?? []).filter(
    (e) =>
      (!q || e.name.toLowerCase().includes(q.toLowerCase())) &&
      (!categoria || e.category === categoria),
  )

  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-2xl font-bold">Cartelera</h1>
      <form
        role="search"
        onSubmit={(e) => e.preventDefault()}
        className="grid gap-4 sm:grid-cols-2 lg:grid-cols-[2fr_1fr_1fr_auto] lg:items-end"
      >
        <Campo
          etiqueta="Buscar por nombre"
          type="search"
          placeholder="Buscar por nombre..."

          value={q}
          onChange={(e) => cambiar('q', e.target.value)}
        />
        <Selector
          etiqueta="Categoría"
          value={categoria ?? ''}
          onChange={(e) => cambiar('categoria', e.target.value)}
        >
          <option value="">Todas</option>
          {EventCategorySchema.options.map((c) => (
            <option key={c} value={c}>
              {CATEGORIAS[c].etiqueta}
            </option>
          ))}
        </Selector>
        <Selector
          etiqueta="Artista"
          value={artista ?? ''}
          onChange={(e) => cambiar('artista', e.target.value)}
        >
          <option value="">Todos</option>
          {artistas.data?.map((a) => (
            <option key={a.id} value={a.stageName}>
              {a.stageName}
            </option>
          ))}
        </Selector>
        <Boton variante="secundario" onClick={() => setParams({}, { replace: true })}>
          Limpiar
        </Boton>
      </form>

      {consulta.isPending && <Cargando />}
      {consulta.isError && (
        <EstadoError error={consulta.error} onReintentar={() => void consulta.refetch()} />
      )}
      {consulta.data && (
        <>
          <p aria-live="polite" className="text-sm text-slate-600 dark:text-slate-400">
            {visibles.length} eventos
          </p>
          {visibles.length === 0 ? (
            <EstadoVacio mensaje="Sin resultados para tu búsqueda" />
          ) : (
            <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {visibles.map((e) => (
                <TarjetaEvento key={e.eventCode} evento={e} />
              ))}
            </div>
          )}
        </>
      )}
    </div>
  )
}
