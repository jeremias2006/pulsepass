import { Link, useParams } from 'react-router'
import { BadgeNeutro } from '../componentes/Badge'
import Cargando from '../componentes/Cargando'
import CodigoTexto from '../componentes/CodigoTexto'
import EstadoError from '../componentes/EstadoError'
import Tarjeta from '../componentes/Tarjeta'
import { useSede } from '../funcionalidades/sedes/hooks'
import { ApiError } from '../lib/http'
import { RUTAS } from '../lib/rutas'
import NoEncontrada from './NoEncontrada'

/**
 * Detalle de una sede (ruta `/sedes/:code`). Caso de uso: ver ciudad y capacidad de un recinto.
 *
 * <p><b>Acceso:</b> pública. <b>Consume:</b> GET /venues/{code}.</p>
 *
 * <p>La capacidad que se muestra aquí es la misma que el backend usa para decidir si un evento
 * se agota (BR-007/008).</p>
 */
export default function DetalleSede() {
  const { code } = useParams()
  const q = useSede(code)
  if (q.isPending) return <Cargando />
  if (q.isError)
    return q.error instanceof ApiError && q.error.status === 404 ? (
      <NoEncontrada mensaje="Sede no encontrada" />
    ) : (
      <EstadoError error={q.error} onReintentar={() => void q.refetch()} />
    )
  const s = q.data
  return (
    <div className="space-y-4">
      <Link to={RUTAS.sedes} className="text-sm font-medium text-marca-700 dark:text-marca-500">
        ← Sedes
      </Link>
      <Tarjeta className="space-y-2">
        <h1 className="font-titulo text-2xl font-bold">{s.name}</h1>
        <p>
          <CodigoTexto>{s.code}</CodigoTexto> ·{' '}
          <BadgeNeutro>{s.active ? 'Activa' : 'Inactiva'}</BadgeNeutro>
        </p>
        <p>{s.city}</p>
        <p>Capacidad: {s.capacity} personas</p>
      </Tarjeta>
    </div>
  )
}
