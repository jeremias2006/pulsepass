import { Link } from 'react-router'
import { BadgeNeutro } from '../componentes/Badge'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import { IconoUbicacion } from '../componentes/iconos'
import Tarjeta from '../componentes/Tarjeta'
import { useSedesActivas } from '../funcionalidades/sedes/hooks'
import { RUTAS } from '../lib/rutas'

/**
 * Lista de sedes (ruta `/sedes`). Caso de uso: ver los recintos donde se celebran los eventos.
 *
 * <p><b>Acceso:</b> pública. <b>Consume:</b> GET /venues/active.</p>
 *
 * <p>Solo se listan las sedes activas: una sede inactiva no admite eventos nuevos (el backend lo
 * revalida al publicar), así que mostrarla al público solo confundiría.</p>
 */
export default function ListaSedes() {
    const q = useSedesActivas()
    return (
        <div className="space-y-6">
            <h1 className="font-titulo text-2xl font-bold">Sedes</h1>
            {q.isPending && <Cargando />}
            {q.isError && <EstadoError error={q.error} onReintentar={() => void q.refetch()} />}
            {q.data?.length === 0 && <EstadoVacio mensaje="No hay sedes activas" />}
            <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
                {q.data?.map((s) => (
                    <Link key={s.code} to={RUTAS.sede(s.code)} className="block">
                        <Tarjeta className="h-full space-y-2 hover:shadow-md">
                            <h2 className="font-titulo font-semibold">{s.name}</h2>
                            <p className="flex items-center gap-2 text-sm">
                                <IconoUbicacion /> {s.city} · Cap. {s.capacity}
                            </p>
                            <BadgeNeutro>Activa</BadgeNeutro>
                        </Tarjeta>
                    </Link>
                ))}
            </div>
        </div>
    )
}