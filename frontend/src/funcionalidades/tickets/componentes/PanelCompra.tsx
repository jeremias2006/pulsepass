import { useState } from 'react'
import { Link } from 'react-router'
import Boton from '../../../componentes/Boton'
import Tarjeta from '../../../componentes/Tarjeta'
import { RUTAS } from '../../../lib/rutas'
import { useAuth } from '../../auth/hooks'
import type { Evento } from '../../eventos/tipos'
import { ETIQUETA_TIPO } from '../etiquetas'
import { TicketTypeSchema, type TicketType } from '../tipos'
import ModalConfirmarCompra from './ModalConfirmarCompra'

/** Solo UX: el backend decide (rol USER, estado, edad, cupo). STAFF/ADMIN no ven el panel (FR-SEC-TKT-01). */
export default function PanelCompra({ evento }: { evento: Evento }) {
  const { sesion, tieneRol } = useAuth()
  const [tipo, setTipo] = useState<TicketType>('GENERAL')
  const [modal, setModal] = useState(false)

  if (sesion.estado === 'autenticado' && !tieneRol('USER')) return null
  const vendible = evento.status === 'PUBLISHED'

  return (
    <>
      <Tarjeta>
        <h2 className="font-titulo text-lg font-semibold">Entradas</h2>
        <fieldset className="mt-3 space-y-2">
          <legend className="text-sm font-medium">Tipo de entrada</legend>
          {TicketTypeSchema.options.map((t) => (
            <label key={t} className="flex min-h-11 items-center gap-3">
              <input
                type="radio"
                name="tipo"
                value={t}
                checked={tipo === t}
                onChange={() => setTipo(t)}
              />
              {ETIQUETA_TIPO[t]}
            </label>
          ))}
        </fieldset>

        {sesion.estado === 'autenticado' ? (
          <Boton className="mt-4 w-full" disabled={!vendible} onClick={() => setModal(true)}>
            Comprar entrada
          </Boton>
        ) : (
          // Anónimo: va a /login y vuelve al evento (state.desde).
          <Link
            to={RUTAS.login}
            state={{ desde: RUTAS.evento(evento.eventCode) }}
            className="mt-4 inline-flex min-h-11 w-full items-center justify-center rounded-lg bg-marca-600 px-4 font-semibold text-white"
          >
            Inicia sesión para comprar
          </Link>
        )}
        {!vendible && (
          <p className="mt-2 text-sm text-amber-700 dark:text-amber-400">
            La compra no está disponible para este evento.
          </p>
        )}
        <p className="mt-2 text-xs text-slate-500 dark:text-slate-400">
          El precio lo define el sistema al confirmar.
        </p>
      </Tarjeta>

      {sesion.estado === 'autenticado' && (
        <ModalConfirmarCompra
          abierto={modal}
          evento={evento}
          tipo={tipo}
          emailSesion={sesion.usuario.email}
          onCerrar={() => setModal(false)}
        />
      )}
    </>
  )
}
