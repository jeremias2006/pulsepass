import { Link, useParams } from 'react-router'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import { useArtista } from '../funcionalidades/artistas/hooks'
import TarjetaEvento from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPorArtista } from '../funcionalidades/eventos/hooks'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'

/**
 * Detalle de un artista (ruta `/artistas/:id`). Caso de uso: ver un artista y en qué eventos
 * participa.
 *
 * <p><b>Acceso:</b> pública. <b>Consume:</b> GET /artists/{id} y, encadenado,
 * GET /events/by-artist.</p>
 *
 * <p>Las dos consultas van en cadena porque el segundo endpoint busca por nombre artístico, no
 * por id: hay que conocer primero el artista para poder pedir sus eventos.</p>
 */
export default function DetalleArtista() {
  const { id } = useParams()
  const n = Number(id)
  // El id viene de la URL, así que puede ser cualquier cosa ("/artistas/abc"). Validarlo aquí
  // evita una llamada al backend condenada a fallar y muestra el 404 al instante.
  const idValido = Number.isInteger(n) && n > 0 ? n : null // id no numérico → 404 de UI sin llamar a la API
  const artista = useArtista(idValido)
  const eventos = useEventosPorArtista(artista.data?.stageName ?? null) // el stageName sale de ArtistResponse

  if (idValido === null) return <NoEncontrada mensaje="Artista no encontrado" />
  if (artista.isPending) return <Cargando />
  if (artista.isError) {
    return artista.error instanceof ApiError && artista.error.status === 404 ? (
      <NoEncontrada mensaje="Artista no encontrado" />
    ) : (
      <EstadoError error={artista.error} onReintentar={() => void artista.refetch()} />
    )
  }
  return (
    <div className="space-y-6">
      <Link to={RUTAS.artistas} className="text-sm font-medium text-marca-700 dark:text-marca-500">
        ← Artistas
      </Link>
      <h1 className="font-titulo text-3xl font-bold">{artista.data.stageName}</h1>
      <section aria-labelledby="prox">
        <h2 id="prox" className="mb-4 font-titulo text-xl font-semibold">
          Próximos eventos de {artista.data.stageName}
        </h2>
        {eventos.isPending && <Cargando />}
        {eventos.isError && <EstadoError error={eventos.error} />}
        {eventos.data?.length === 0 && <EstadoVacio mensaje="Sin eventos próximos" />}
        <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
          {eventos.data?.map((e) => (
            <TarjetaEvento key={e.eventCode} evento={e} />
          ))}
        </div>
      </section>
    </div>
  )
}
