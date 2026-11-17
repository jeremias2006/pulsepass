import { Link } from 'react-router'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import AvatarArtista from '../funcionalidades/artistas/componentes/AvatarArtista'
import { useArtistasActivos } from '../funcionalidades/artistas/hooks'
import { useAuth } from '../funcionalidades/auth/hooks'
import TarjetaEvento from '../funcionalidades/eventos/componentes/TarjetaEvento'
import { useEventosPublicados } from '../funcionalidades/eventos/hooks'
import { RUTAS } from '../lib/rutas'

// Inicio es la única página (junto con el Layout) que NO se carga de forma perezosa (FE-012).
export default function Inicio() {
  const { sesion } = useAuth()
  const eventos = useEventosPublicados()
  const artistas = useArtistasActivos()
  return (
    <div>
      <section className="bg-linear-to-r from-marca-600 to-acento-500 text-white">
        <div className="mx-auto max-w-6xl px-4 py-14 md:py-20">
          <h1 className="max-w-xl font-titulo text-4xl font-bold md:text-5xl">
            Descubre los mejores eventos
          </h1>
          <p className="mt-4 max-w-xl text-lg">
            Música, cultura, deporte y tecnología en un solo lugar.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link
              to={RUTAS.eventos}
              className="inline-flex min-h-11 items-center rounded-lg bg-white px-5 font-semibold text-marca-700 dark:text-marca-500"
            >
              Ver cartelera
            </Link>
            {sesion.estado !== 'autenticado' && (
              <Link
                to={RUTAS.registro}
                className="inline-flex min-h-11 items-center rounded-lg border border-white px-5 font-semibold"
              >
                Crear cuenta
              </Link>
            )}
          </div>
        </div>
      </section>

      <div className="mx-auto max-w-6xl space-y-12 px-4 py-10">
        <section aria-labelledby="proximos">
          <div className="mb-4 flex items-baseline justify-between">
            <h2 id="proximos" className="font-titulo text-xl font-semibold">
              Próximos eventos
            </h2>
            <Link
              to={RUTAS.eventos}
              className="text-sm font-medium text-marca-700 dark:text-marca-500"
            >
              Ver todos →
            </Link>
          </div>
          {eventos.isPending && <Cargando />}
          {eventos.isError && (
            <EstadoError error={eventos.error} onReintentar={() => void eventos.refetch()} />
          )}
          {eventos.data?.length === 0 && <EstadoVacio mensaje="Aún no hay eventos publicados" />}
          <div className="grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
            {eventos.data?.slice(0, 3).map((e) => (
              <TarjetaEvento key={e.eventCode} evento={e} />
            ))}
          </div>
        </section>

        <section aria-labelledby="destacados">
          <div className="mb-4 flex items-baseline justify-between">
            <h2 id="destacados" className="font-titulo text-xl font-semibold">
              Artistas destacados
            </h2>
            <Link
              to={RUTAS.artistas}
              className="text-sm font-medium text-marca-700 dark:text-marca-500"
            >
              Ver todos →
            </Link>
          </div>
          {artistas.isError && (
            <EstadoError error={artistas.error} onReintentar={() => void artistas.refetch()} />
          )}
          <div className="flex flex-wrap gap-4">
            {artistas.data?.slice(0, 6).map((a) => (
              <AvatarArtista key={a.id} artista={a} orientacion="horizontal" />
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
