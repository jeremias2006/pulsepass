import { useState } from 'react'
import { Link } from 'react-router'
import Cargando from '../componentes/Cargando'
import EstadoError from '../componentes/EstadoError'
import EstadoVacio from '../componentes/EstadoVacio'
import FiltroChips from '../componentes/FiltroChips'
import { useAuth } from '../funcionalidades/auth/hooks'
import TarjetaTicket from '../funcionalidades/tickets/componentes/TarjetaTicket'
import { useMisTickets } from '../funcionalidades/tickets/hooks'
import { RUTAS } from '../lib/rutas'

type Filtro = 'TODOS' | 'PAID' | 'USED' | 'CANCELLED'

const OPCIONES: { valor: Filtro; etiqueta: string }[] = [
  { valor: 'TODOS', etiqueta: 'Todos' },
  { valor: 'PAID', etiqueta: 'Pagados' },
  { valor: 'USED', etiqueta: 'Usados' },
  { valor: 'CANCELLED', etiqueta: 'Cancelados' },
]

export default function MisTickets() {
  const { sesion } = useAuth()
  // GAP-03: no existe consulta por ID de usuario; se usa el correo que devuelve /auth/me.
  const email = sesion.estado === 'autenticado' ? sesion.usuario.email : undefined
  const q = useMisTickets(email)
  const [filtro, setFiltro] = useState<Filtro>('TODOS')

  const visibles = (q.data ?? []).filter((t) => filtro === 'TODOS' || t.status === filtro)

  return (
    <div className="space-y-6">
      <h1 className="font-titulo text-2xl font-bold">Mis tickets</h1>
      <FiltroChips
        etiquetaGrupo="Filtrar por estado"
        opciones={OPCIONES}
        valor={filtro}
        onCambiar={setFiltro}
      />
      {q.isPending && <Cargando />}
      {q.isError && <EstadoError error={q.error} onReintentar={() => void q.refetch()} />}
      {q.data &&
        (visibles.length === 0 ? (
          <EstadoVacio
            mensaje="Aún no tienes tickets"
            accion={
              <Link to={RUTAS.eventos} className="font-medium text-marca-700 dark:text-marca-500">
                Ver cartelera
              </Link>
            }
          />
        ) : (
          <div className="space-y-4">
            {visibles.map((t) => (
              <TarjetaTicket key={t.ticketCode} ticket={t} />
            ))}
          </div>
        ))}
    </div>
  )
}
