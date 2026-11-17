import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import AvatarArtista from '../funcionalidades/artistas/componentes/AvatarArtista'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'

export default function ListaArtistas() {
  const q = useArtistasActivos()
  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-2xl font-bold">Artistas</h1>
      {q.isPending && <Cargando />}
      {q.isError && <EstadoError error={q.error} onReintentar={() => void q.refetch()} />}
      {q.data?.length === 0 && <EstadoVacio mensaje="No hay artistas activos" />}
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {q.data?.map((a) => (
          <AvatarArtista key={a.id} artista={a} />
        ))}
      </div>
    </div>
  )
}
